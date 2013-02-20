package net.rubyeye.xmemcached.test.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.junit.Ignore;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.core.impl.TextLineCodecFactory;
import com.google.code.yanf4j.nio.TCPController;

/**
 * Unit test for failure mode
 * 
 * @author dennis
 * @date 2010-12-28
 */
@Ignore
public class FailureModeUnitTest {
	/**
	 * Mock handler for memcached server
	 * 
	 * @author dennis
	 * @date 2010-12-28
	 */
	private static final class MockHandler extends HandlerAdapter {
		private final String response;

		public MockHandler(String response) {
			super();
			this.response = response;
		}

		@Override
		public void onMessageReceived(Session session, Object message) {
			String line = (String) message;
			String key = line.split(" ")[1];
			session.write("VALUE " + key + " 0 " + this.response.length());
			session.write(this.response);
			session.write("END");
		}

	}

	@Test
	public void testFailureMode_HasStandbyNode() throws Exception {
		TCPController memServer1 = new TCPController();
		memServer1.setHandler(new MockHandler("response from server1"));
		memServer1.setCodecFactory(new TextLineCodecFactory());
		memServer1.bind(new InetSocketAddress(4799));

		TCPController memServer2 = new TCPController();
		memServer2.setHandler(new MockHandler("response from server2"));
		memServer2.setCodecFactory(new TextLineCodecFactory());
		memServer2.bind(new InetSocketAddress(4798));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799,localhost:4798"));
		// It must be in failure mode
		builder.setFailureMode(true);
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);

		try {

			assertEquals("response from server1", client.get("a"));
			assertEquals("response from server1", client.get("a"));
			memServer1.stop();
			Thread.sleep(1000);
			assertEquals("response from server2", client.get("a"));
			// restart server1
			memServer1 = new TCPController();
			memServer1.setHandler(new MockHandler("response from server1"));
			memServer1.setCodecFactory(new TextLineCodecFactory());
			memServer1.bind(new InetSocketAddress(4799));
			Thread.sleep(5000);
			assertEquals("response from server1", client.get("a"));
		} finally {
			memServer1.stop();
			memServer2.stop();
			client.shutdown();
		}

	}

	@Test
	public void testFailureMode_OneServerDownOnStartup() throws Exception {

		TCPController memServer2 = new TCPController();
		memServer2.setHandler(new MockHandler("response from server2"));
		memServer2.setCodecFactory(new TextLineCodecFactory());
		memServer2.bind(new InetSocketAddress(4798));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799 localhost:4798"));
		// It must be in failure mode
		builder.setFailureMode(true);
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);
		TCPController memServer1 = null;
		try {

			assertEquals("response from server2", client.get("a"));
			try {
				assertEquals("response from server1", client.get("b"));
				fail();
			} catch (MemcachedException e) {
				assertEquals("Session(127.0.0.1:4799) has been closed", e
						.getMessage());
			}
			assertEquals(1, client.getConnector().getSessionByAddress(
					AddrUtil.getOneAddress("localhost:4799")).size());
			memServer1 = new TCPController();
			memServer1.setHandler(new MockHandler("response from server1"));
			memServer1.setCodecFactory(new TextLineCodecFactory());
			memServer1.bind(new InetSocketAddress(4799));
			Thread.sleep(5000);
			assertEquals(1, client.getConnector().getSessionByAddress(
					AddrUtil.getOneAddress("localhost:4799")).size());
			assertEquals("response from server2", client.get("a"));
			assertEquals("response from server1", client.get("b"));
		} finally {
			if (memServer1 != null)
				memServer1.stop();
			memServer2.stop();
			client.shutdown();
		}

	}

	@Test
	public void testFailureMode_StandbyNodeDown_Recover() throws Exception {
		TCPController memServer1 = new TCPController();
		memServer1.setHandler(new MockHandler("response from server1"));
		memServer1.setCodecFactory(new TextLineCodecFactory());
		memServer1.bind(new InetSocketAddress(4799));

		TCPController memServer2 = new TCPController();
		memServer2.setHandler(new MockHandler("response from server2"));
		memServer2.setCodecFactory(new TextLineCodecFactory());
		memServer2.bind(new InetSocketAddress(4798));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799,localhost:4798"));
		// It must be in failure mode
		builder.setFailureMode(true);
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);

		try {
			assertEquals("response from server1", client.get("a"));
			assertEquals("response from server1", client.get("a"));
			memServer1.stop();
			Thread.sleep(1000);
			assertEquals("response from server2", client.get("a"));
			memServer2.stop();
			Thread.sleep(1000);
			try {
				client.get("a");
				fail();
			} catch (MemcachedException e) {
				assertEquals("Session(127.0.0.1:4799) has been closed", e
						.getMessage());
				// e.printStackTrace();
			}
			// restart server2
			memServer2 = new TCPController();
			memServer2.setHandler(new MockHandler("response from server2"));
			memServer2.setCodecFactory(new TextLineCodecFactory());
			memServer2.bind(new InetSocketAddress(4798));
			Thread.sleep(5000);
			assertEquals("response from server2", client.get("a"));

			// restart server1
			memServer1 = new TCPController();
			memServer1.setHandler(new MockHandler("response from server1"));
			memServer1.setCodecFactory(new TextLineCodecFactory());
			memServer1.bind(new InetSocketAddress(4799));
			Thread.sleep(10000);
			assertEquals("response from server1", client.get("a"));

		} finally {
			memServer1.stop();
			memServer2.stop();
			client.shutdown();
		}

	}

	@Test
	public void testFailureMode_NoStandbyNode() throws Exception {
		TCPController memServer1 = new TCPController();
		memServer1.setHandler(new MockHandler("response from server1"));
		memServer1.setCodecFactory(new TextLineCodecFactory());
		memServer1.bind(new InetSocketAddress(4799));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799"));
		builder.setFailureMode(true);
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);

		try {

			assertEquals("response from server1", client.get("a"));
			assertEquals("response from server1", client.get("a"));
			memServer1.stop();
			Thread.sleep(1000);
			try {
				client.get("a");
				fail();
			} catch (MemcachedException e) {
				assertEquals("Session(127.0.0.1:4799) has been closed", e
						.getMessage());
				assertTrue(true);
			}
		} finally {
			memServer1.stop();
			client.shutdown();
		}

	}

	@Test
	public void testNotFailureMode_HasStandbyNode() throws Exception {
		TCPController memServer1 = new TCPController();
		memServer1.setHandler(new MockHandler("response from server1"));
		memServer1.setCodecFactory(new TextLineCodecFactory());
		memServer1.bind(new InetSocketAddress(4799));

		TCPController memServer2 = new TCPController();
		memServer2.setHandler(new MockHandler("response from server2"));
		memServer2.setCodecFactory(new TextLineCodecFactory());
		memServer2.bind(new InetSocketAddress(4798));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799,localhost:4798"));
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);
		try {

			assertEquals("response from server1", client.get("a"));
			assertEquals("response from server1", client.get("a"));
			memServer1.stop();
			Thread.sleep(1000);
			try {
				client.get("a");
				fail();
			} catch (MemcachedException e) {
				assertEquals("There is no available connection at this moment",
						e.getMessage());
				assertTrue(true);
			}
		} finally {
			memServer1.stop();
			memServer2.stop();
			client.shutdown();
		}

	}

	@Test
	public void testNotFailureMode_NoStandbyNode() throws Exception {
		TCPController memServer1 = new TCPController();
		memServer1.setHandler(new MockHandler("response from server1"));
		memServer1.setCodecFactory(new TextLineCodecFactory());
		memServer1.bind(new InetSocketAddress(4799));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799"));
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);
		try {

			assertEquals("response from server1", client.get("a"));
			assertEquals("response from server1", client.get("a"));
			memServer1.stop();
			Thread.sleep(1000);
			try {
				client.get("a");
				fail();
			} catch (MemcachedException e) {
				assertEquals("There is no available connection at this moment",
						e.getMessage());
				assertTrue(true);
			}
		} finally {
			memServer1.stop();
			client.shutdown();
		}

	}

	@Test
	public void testNotFailureMode_NoStandbyNode_TwoServers() throws Exception {
		TCPController memServer1 = new TCPController();
		memServer1.setHandler(new MockHandler("response from server1"));
		memServer1.setCodecFactory(new TextLineCodecFactory());
		memServer1.bind(new InetSocketAddress(4799));

		TCPController memServer2 = new TCPController();
		memServer2.setHandler(new MockHandler("response from server2"));
		memServer2.setCodecFactory(new TextLineCodecFactory());
		memServer2.bind(new InetSocketAddress(4798));

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddressMap("localhost:4799 localhost:4798"));
		MemcachedClient client = builder.build();

		client.setEnableHeartBeat(false);

		try {

			assertEquals("response from server2", client.get("a"));
			assertEquals("response from server2", client.get("a"));
			memServer2.stop();
			Thread.sleep(1000);
			assertEquals("response from server1", client.get("a"));
		} finally {
			memServer1.stop();
			memServer2.stop();
			client.shutdown();
		}

	}

}
