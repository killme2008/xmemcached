package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPController;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class ConnectionPoolMemcachedClientTest extends XMemcachedClientTest {
	private static final int CONNECTION_POOL_SIZE = 3;

	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(this.properties
						.getProperty("test.memcached.servers")));
		builder.setConnectionPoolSize(CONNECTION_POOL_SIZE);
		return builder;
	}

	/**
	 * Mock memcached server
	 * 
	 * @author boyan
	 * 
	 */
	static class MockServer {
		TCPController controller;
		AtomicInteger sessionCounter = new AtomicInteger(0);
		private InetSocketAddress localAddress;

		public void start() {
			controller = new TCPController();
			controller.setHandler(new HandlerAdapter() {

				@Override
				public void onSessionClosed(Session session) {
				}

				@Override
				public void onSessionCreated(Session session) {
					System.out.println("connection created,"
							+ sessionCounter.incrementAndGet());
				}

			});
			try {
				if (localAddress != null)
					controller.bind(localAddress);
				else
					controller.bind(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public InetSocketAddress getServerAddress() {
			return this.controller.getLocalSocketAddress();
		}

		public void stop() {
			try {
				this.controller.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void testHealSession() throws Exception {
		MockServer server = new MockServer();
		server.start();
		InetSocketAddress serverAddress = server.getServerAddress();
		XMemcachedClient client = new XMemcachedClient();
		client.setConnectionPoolSize(5);
		client.addServer(serverAddress);
		assertEquals(1, client.getAvaliableServers().size());
		assertEquals(5, client.getConnectionSizeBySocketAddress(serverAddress));
		assertEquals(5, server.sessionCounter.get());

		// stop mock server,try to heal sessions
		server.stop();

		Thread.sleep(5000);
		assertEquals(0, client.getAvaliableServers().size());
		assertEquals(0, client.getConnectionSizeBySocketAddress(serverAddress));
		// new server start
		server = new MockServer();
		server.localAddress=serverAddress;
		server.start();
		Thread.sleep(5000);
		assertEquals(1, client.getAvaliableServers().size());
		assertEquals(5, client.getConnectionSizeBySocketAddress(serverAddress));
		assertEquals(5, server.sessionCounter.get());

		server.stop();
		client.shutdown();

	}

	@Override
	public MemcachedClientBuilder createWeightedBuilder() throws Exception {
		List<InetSocketAddress> addressList = AddrUtil
				.getAddresses(this.properties
						.getProperty("test.memcached.servers"));
		int[] weights = new int[addressList.size()];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = i + 1;
		}

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(
				addressList, weights);
		builder.setConnectionPoolSize(CONNECTION_POOL_SIZE);
		builder.setCommandFactory(new BinaryCommandFactory());
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		return builder;
	}
}
