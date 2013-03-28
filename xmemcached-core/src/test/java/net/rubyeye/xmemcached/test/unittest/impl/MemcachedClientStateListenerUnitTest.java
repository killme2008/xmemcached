package net.rubyeye.xmemcached.test.unittest.impl;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

import com.google.code.yanf4j.util.ResourcesUtils;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import junit.framework.TestCase;

public class MemcachedClientStateListenerUnitTest extends TestCase {

	MemcachedClient memcachedClient;
	MockMemcachedClientStateListener listener;

	@Override
	protected void setUp() throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder();
		listener = new MockMemcachedClientStateListener();
		builder.addStateListener(listener);
		memcachedClient = builder.build();
	}

	public void testStarted() {
		assertEquals(1, listener.getNum());
	}

	public void testShutDown() throws Exception {
		memcachedClient.shutdown();
		assertEquals(2, listener.getNum());
	}

	public void testRemoveListener() throws Exception {
		assertEquals(1, listener.getNum());
		memcachedClient.removeStateListener(this.listener);
		assertEquals(0, memcachedClient.getStateListeners().size());
		memcachedClient.shutdown();
		assertEquals(1, listener.getNum());
	}

	public void testAddListener() {
		assertEquals(1, memcachedClient.getStateListeners().size());
		memcachedClient.addStateListener(listener);
		assertEquals(2, memcachedClient.getStateListeners().size());
		memcachedClient.addStateListener(listener);
		assertEquals(3, memcachedClient.getStateListeners().size());

		memcachedClient.removeStateListener(this.listener);
		assertEquals(0, memcachedClient.getStateListeners().size());
	}

	public void testConnected() throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("test.properties");
		String serversString = properties.getProperty("test.memcached.servers");
		List<InetSocketAddress> list = AddrUtil.getAddresses(serversString);
		memcachedClient.addServer(serversString);
		synchronized (this) {
			while (memcachedClient.getAvaliableServers().size() < list.size())
				wait(1000);
		}
		assertEquals(1 + memcachedClient.getAvaliableServers().size(), listener
				.getNum());
	}

	public void testDisconnected() throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("test.properties");
		String serversString = properties.getProperty("test.memcached.servers");
		List<InetSocketAddress> list = AddrUtil.getAddresses(serversString);
		memcachedClient.addServer(serversString);
		synchronized (this) {
			while (memcachedClient.getAvaliableServers().size() < list.size())
				wait(1000);
		}
		int serverCount = memcachedClient.getAvaliableServers().size();
		Thread.sleep(2000);
		memcachedClient.shutdown();
		synchronized (this) {
			
			while (memcachedClient.getAvaliableServers().size() > 0){
				//System.out.println(memcachedClient.getAvaliableServers().size());
				wait(1000);
			}
		}
		assertEquals(2 + 2 * serverCount, listener.getNum());
	}

	@Override
	protected void tearDown() throws Exception {
		memcachedClient.shutdown();
	}

}
