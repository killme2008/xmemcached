package net.rubyeye.xmemcached.test.unittest.monitor;

import java.lang.reflect.Method;

import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import junit.framework.TestCase;

public class XMemcachedMBeanServerUnitTest extends TestCase {
	Mock mock;

	public void setUp() {
		System.setProperty(Constants.XMEMCACHED_JMX_ENABLE, "true");
		this.mock = new Mock();
	}

	public void testMBeanServer() throws Exception {
		Method method = XMemcachedMbeanServer.getInstance().getClass()
				.getDeclaredMethod("initialize", new Class[] {});
		method.setAccessible(true);
		method.invoke(XMemcachedMbeanServer.getInstance());

		assertTrue(XMemcachedMbeanServer.getInstance().isActive());
		int oldCount = XMemcachedMbeanServer.getInstance().getMBeanCount();
		String name = mock.getClass().getPackage().getName() + ":type="
				+ mock.getClass().getSimpleName();
		XMemcachedMbeanServer.getInstance().registMBean(mock, name);
		assertEquals(oldCount + 1, XMemcachedMbeanServer.getInstance()
				.getMBeanCount());
		assertTrue(XMemcachedMbeanServer.getInstance().isRegistered(name));
		XMemcachedMbeanServer.getInstance().shutdown();
		assertFalse(XMemcachedMbeanServer.getInstance().isActive());
	}

	public void setup() {
		System.setProperty(Constants.XMEMCACHED_JMX_ENABLE, "false");
	}

}
