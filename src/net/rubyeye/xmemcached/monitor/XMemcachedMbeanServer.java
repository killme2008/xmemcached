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

import org.apache.log4j.Logger;

/**
 * @author dennis
 * 
 */
public final class XMemcachedMbeanServer {
	private static final Logger log = Logger
			.getLogger(XMemcachedMbeanServer.class);

	private MBeanServer mbserver = null;

	private static XMemcachedMbeanServer instance = new XMemcachedMbeanServer();

	private XMemcachedMbeanServer() {
		// 创建MBServer
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
			boolean useJmx = Boolean.parseBoolean(System.getProperty(
					Constants.XMEMCACHED_JMX_ENABLE, "false"));
			System.out.println(useJmx);
			if (useJmx) {
				mbserver = ManagementFactory.getPlatformMBeanServer();
				int port = Integer.parseInt(System.getProperty(
						Constants.XMEMCACHED_RMI_PORT, "7077"));
				String rmiName = System.getProperty(
						Constants.XMEMCACHED_RMI_NAME, "xmemcachedServer");
				Registry reg = null;
				try {
					reg = LocateRegistry.getRegistry(port);
					reg.list();
				} catch (Exception e) {
					reg = null;
				}
				if (null == reg) {
					reg = LocateRegistry.createRegistry(port);
				}
				reg.list();
				String serverURL = "service:jmx:rmi:///jndi/rmi://" + host
						+ ":" + port + "/" + rmiName;
				JMXServiceURL url = new JMXServiceURL(serverURL);
				final JMXConnectorServer connectorServer = JMXConnectorServerFactory
						.newJMXConnectorServer(url, null, mbserver);
				connectorServer.start();
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							System.err.println("JMXConnector stop");
							connectorServer.stop();
						} catch (IOException e) {
							log.error(e);
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

	public void registMBean(Object o, String name) {
		// 注册MBean
		if (mbserver != null) {
			try {
				mbserver.registerMBean(o, new ObjectName(name));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
