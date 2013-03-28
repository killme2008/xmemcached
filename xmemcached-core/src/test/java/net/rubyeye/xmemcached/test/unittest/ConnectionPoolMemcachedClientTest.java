package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPController;

public class ConnectionPoolMemcachedClientTest extends XMemcachedClientTest {
	private static final int CONNECTION_POOL_SIZE = 3;

	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(properties
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
		private static final int PORT = 9999;
		TCPController controller;
		AtomicInteger sessionCounter = new AtomicInteger(0);

		public void start() {
			controller = new TCPController();
			controller.setHandler(new HandlerAdapter() {

				@Override
				public void onSessionClosed(Session session) {
					sessionCounter.decrementAndGet();
				}

				@Override
				public void onSessionCreated(Session session) {
					System.out.println("connection created,"
							+ sessionCounter.incrementAndGet());
				}

			});
			try {
				controller.bind(PORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public InetSocketAddress getServerAddress() {
			return new InetSocketAddress("localhost", PORT);
		}

		public void stop() {
			try {
				controller.stop();
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
		client.setEnableHeartBeat(false);
		client.addServer(serverAddress);
		synchronized (this) {
			while (server.sessionCounter.get() < 5) {
				this.wait(1000);
			}
		}
		assertEquals(1, client.getAvaliableServers().size());
		assertEquals(5, client.getConnectionSizeBySocketAddress(serverAddress));
		assertEquals(5, server.sessionCounter.get());

		// stop mock server,try to heal sessions
		server.stop();

		Thread.sleep(10000);
		assertEquals(0, client.getAvaliableServers().size());
		assertEquals(0, client.getConnectionSizeBySocketAddress(serverAddress));
		// new server start
		server = new MockServer();
		server.start();
		Thread.sleep(30000);
		assertEquals(1, client.getAvaliableServers().size());
		assertEquals(5, client.getConnectionSizeBySocketAddress(serverAddress));
		assertEquals(5, server.sessionCounter.get());

		server.stop();
		client.shutdown();

	}
	
	public void testDisableHealSession() throws Exception {
		MockServer server = new MockServer();
		server.start();
		InetSocketAddress serverAddress = server.getServerAddress();
		XMemcachedClient client = new XMemcachedClient();
		client.setConnectionPoolSize(5);
		client.setEnableHeartBeat(false);
		//disable heal session.
		client.setEnableHealSession(false);
		client.addServer(serverAddress);
		synchronized (this) {
			while (server.sessionCounter.get() < 5) {
				this.wait(1000);
			}
		}
		assertEquals(1, client.getAvaliableServers().size());
		assertEquals(5, client.getConnectionSizeBySocketAddress(serverAddress));
		assertEquals(5, server.sessionCounter.get());

		// stop mock server,try to heal sessions
		server.stop();

		Thread.sleep(10000);
		assertEquals(0, client.getAvaliableServers().size());
		assertEquals(0, client.getConnectionSizeBySocketAddress(serverAddress));
		// new server start
		server = new MockServer();
		server.start();
		Thread.sleep(30000);
		//Still empty.
		assertEquals(0, client.getAvaliableServers().size());
		assertEquals(0, client.getConnectionSizeBySocketAddress(serverAddress));

		server.stop();
		client.shutdown();

	}

	@Override
	public MemcachedClientBuilder createWeightedBuilder() throws Exception {
		List<InetSocketAddress> addressList = AddrUtil
				.getAddresses(properties
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
