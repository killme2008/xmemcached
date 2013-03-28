/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.monitor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enable JMX supports,default is false: </br>
 * 
 * &nbsp;&nbsp;java -Dxmemcached.jmx.enable=true -Dxmemcached.rmi.port=7077
 * -Dxmemcached.rmi.name=xmemcachedServer </br>
 * 
 * Access MBean through: </br>
 * 
 * &nbsp;&nbsp;service:jmx:rmi:///jndi/rmi://[host]:7077/xmemcachedServer </br>
 * 
 * You can add or remove memcached server dynamically and monitor
 * XmemcachedClient?'s behavior through MBeans.Other options: </br>
 * <ul>
 * <li>-Dxmemcached.rmi.port</li>
 * <li>-Dxmemcached.rmi.name</li>
 * </ul>
 * 
 * @author dennis
 * 
 */
public final class XMemcachedMbeanServer {
	private static final Logger log = LoggerFactory
			.getLogger(XMemcachedMbeanServer.class);

	private MBeanServer mbserver = null;

	private static XMemcachedMbeanServer instance = new XMemcachedMbeanServer();
	private JMXConnectorServer connectorServer;

	private Thread shutdownHookThread;
	private volatile boolean isHutdownHookCalled = false;

	private XMemcachedMbeanServer() {
		initialize();
	}

	private void initialize() {
		if (mbserver != null && connectorServer != null
				&& connectorServer.isActive()) {
			return;
		}
		// 鍒涘缓MBServer
		String hostName = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();

			hostName = addr.getHostName();
		} catch (IOException e) {
			log.error("Get HostName Error", e);
			hostName = "localhost";
		}
		String host = System.getProperty("hostName", hostName);
		try {
			boolean enableJMX = Boolean.parseBoolean(System.getProperty(
					Constants.XMEMCACHED_JMX_ENABLE, "false"));
			if (enableJMX) {
				mbserver = ManagementFactory.getPlatformMBeanServer();
				int port = Integer.parseInt(System.getProperty(
						Constants.XMEMCACHED_RMI_PORT, "7077"));
				String rmiName = System.getProperty(
						Constants.XMEMCACHED_RMI_NAME, "xmemcachedServer");
				Registry registry = null;
				try {
					registry = LocateRegistry.getRegistry(port);
					registry.list();
				} catch (Exception e) {
					registry = null;
				}
				if (null == registry) {
					registry = LocateRegistry.createRegistry(port);
				}
				registry.list();
				String serverURL = "service:jmx:rmi:///jndi/rmi://" + host
						+ ":" + port + "/" + rmiName;
				JMXServiceURL url = new JMXServiceURL(serverURL);
				connectorServer = JMXConnectorServerFactory
						.newJMXConnectorServer(url, null, mbserver);
				connectorServer.start();
				shutdownHookThread = new Thread() {
					@Override
					public void run() {
						try {
							isHutdownHookCalled = true;
							if (connectorServer
									.isActive()) {
								connectorServer
										.stop();
								log.warn("JMXConnector stop");
							}
						} catch (IOException e) {
							log.error("Shutdown Xmemcached MBean server error",
									e);
						}
					}
				};
				
				Runtime.getRuntime().addShutdownHook(shutdownHookThread);
				log.warn("jmx url: " + serverURL);
			}
		} catch (Exception e) {
			log.error("create MBServer error", e);
		}
	}

	public static XMemcachedMbeanServer getInstance() {
		return instance;
	}

	public final void shutdown() {
		try {
			if (connectorServer != null && connectorServer.isActive()) {
				connectorServer.stop();
				log.warn("JMXConnector stop");
				if (!isHutdownHookCalled) {
					Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
				}
			}
		} catch (IOException e) {
			log.error("Shutdown Xmemcached MBean server error", e);
		}
	}

	public boolean isRegistered(String name) {
		try {
			return mbserver != null
					&& mbserver.isRegistered(new ObjectName(name));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isActive() {
		return mbserver != null && connectorServer != null
				&& connectorServer.isActive();
	}

	public int getMBeanCount() {
		if (mbserver != null) {
			return mbserver.getMBeanCount();
		} else {
			return 0;
		}
	}

	public void registMBean(Object o, String name) {
		if (isRegistered(name)) {
			return;
		}
		// 娉ㄥ唽MBean
		if (mbserver != null) {
			try {
				mbserver.registerMBean(o, new ObjectName(name));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
