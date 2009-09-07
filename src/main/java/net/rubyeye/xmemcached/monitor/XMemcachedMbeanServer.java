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
 * jmx MBeanServer锛岄�杩嘡MI鍙戝竷锛屼綘鍙互閫氳繃service:jmx:rmi:///jndi/rmi://[host]:[port]/[
 * name]璁块棶姝ゆ湇鍔�/br>
 * 
 * 榛樿JMX鏈紑鍚紝浣犲彲浠ラ�杩囧惎鍔ㄥ弬鏁癹ava
 * -Dxmemcached.jmx.enable=true鏉ュ惎鐢紝榛樿鐨刾ort鏄�077锛岄粯璁ょ殑name鏄痻memcachedServer
 * 杩欎簺鍙傛暟鍙互閫氳繃涓嬪垪鍙傛暟鏉ヤ慨鏀�</br>
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

	private XMemcachedMbeanServer() {
		initialize();
	}

	private void initialize() {
		if (this.mbserver != null && this.connectorServer != null
				&& this.connectorServer.isActive()) {
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
				this.mbserver = ManagementFactory.getPlatformMBeanServer();
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
				this.connectorServer = JMXConnectorServerFactory
						.newJMXConnectorServer(url, null, this.mbserver);
				this.connectorServer.start();
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {

							if (XMemcachedMbeanServer.this.connectorServer.isActive()) {
								XMemcachedMbeanServer.this.connectorServer.stop();
								log.warn("JMXConnector stop");
							}
						} catch (IOException e) {
							log.error("Shutdown Xmemcached MBean server error",e);
						}
					}
				});
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
			if (this.connectorServer != null && this.connectorServer.isActive()) {
				this.connectorServer.stop();
				log.warn("JMXConnector stop");
			}
		} catch (IOException e) {
			log.error("Shutdown Xmemcached MBean server error",e);
		}
	}

	public boolean isRegistered(String name) {
		try {
			return this.mbserver != null
					&& this.mbserver.isRegistered(new ObjectName(name));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isActive() {
		return this.mbserver != null && this.connectorServer != null
				&& this.connectorServer.isActive();
	}

	public int getMBeanCount() {
		if (this.mbserver != null) {
			return this.mbserver.getMBeanCount();
		} else {
			return 0;
		}
	}

	public void registMBean(Object o, String name) {
		if (isRegistered(name)) {
			return;
		}
		// 娉ㄥ唽MBean
		if (this.mbserver != null) {
			try {
				this.mbserver.registerMBean(o, new ObjectName(name));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
