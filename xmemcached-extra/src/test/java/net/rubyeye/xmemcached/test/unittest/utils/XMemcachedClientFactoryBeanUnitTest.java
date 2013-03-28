package net.rubyeye.xmemcached.test.unittest.utils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XMemcachedClientFactoryBeanUnitTest extends TestCase {

	ApplicationContext ctx;

	@Override
	public void setUp() throws Exception {
		this.ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
	}

	public void testSimpleConfig() throws Exception {
		MemcachedClient memcachedClient = (MemcachedClient) this.ctx
				.getBean("memcachedClient1");

		validateClient(memcachedClient);
	}

	public void testAllConfig() throws Exception {
		MemcachedClient memcachedClient = (MemcachedClient) this.ctx
				.getBean("memcachedClient2");
		validateClient(memcachedClient);
	}

	public void testComposite() throws Exception {
		MemcachedClient memcachedClient1 = (MemcachedClient) this.ctx
				.getBean("memcachedClient1");
		MemcachedClient memcachedClient2 = (MemcachedClient) this.ctx
				.getBean("memcachedClient2");
		validateClient(memcachedClient1);
		memcachedClient1.flushAll();
		validateClient(memcachedClient2);
	}

	private void validateClient(MemcachedClient memcachedClient)
			throws TimeoutException, InterruptedException, MemcachedException,
			IOException {
		// memcachedClient.setLoggingLevelVerbosity(new InetSocketAddress(
		// "localhost", 12000), 3);
		assertNotNull(memcachedClient);
		assertTrue(memcachedClient.getConnector().isStarted());
		assertFalse(memcachedClient.isShutdown());
		memcachedClient.set("test", 0, 1, 1000000);
		assertEquals(1, memcachedClient.get("test"));
		memcachedClient.shutdown();
	}

}
