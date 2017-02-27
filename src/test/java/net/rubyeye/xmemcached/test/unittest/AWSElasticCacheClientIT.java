package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

import net.rubyeye.xmemcached.aws.AWSElasticCacheClient;
import net.rubyeye.xmemcached.aws.ClusterConfigration;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.junit.Test;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.core.impl.TextLineCodecFactory;
import com.google.code.yanf4j.nio.TCPController;
import com.google.code.yanf4j.util.ResourcesUtils;

import junit.framework.TestCase;

public class AWSElasticCacheClientIT extends TestCase {

	private String serverList;
	private List<InetSocketAddress> addresses;

	/**
	 * elasticcache config node mock handler
	 * 
	 * @author dennis
	 *
	 */
	private static final class MockHandler extends HandlerAdapter {
		private final String response;
		private int version;

		public MockHandler(int version, String response) {
			super();
			this.response = response;
			this.version = version;
		}

		@Override
		public void onMessageReceived(Session session, Object message) {
			if (message.equals("quit")) {
				session.close();
				return;
			}
			session.write("CONFIG cluster 0 " + this.response.length());
			session.write(String.valueOf(version) + "\n" + this.response);
			session.write("END");
			this.version++;
		}

	}

	@Override
	public void setUp() throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("test.properties");
		List<InetSocketAddress> addresses = AddrUtil.getAddresses(properties
				.getProperty("test.memcached.servers"));
		StringBuffer sb = new StringBuffer();
		boolean wasFirst = true;
		for (InetSocketAddress addr : addresses) {
			if (wasFirst) {
				wasFirst = false;
			} else {
				sb.append(" ");
			}
			sb.append(addr.getHostString() + "|" + addr.getHostString() + "|"
					+ addr.getPort());

		}

		this.addresses = addresses;
		serverList = sb.toString();
	}

	@Test
	public void testInvalidConfig() throws Exception {
		TCPController configServer = new TCPController();
		int version = 10;
		configServer.setHandler(new MockHandler(version, "invalid"));
		configServer.setCodecFactory(new TextLineCodecFactory());
		configServer.bind(new InetSocketAddress(2271));

		try {
			AWSElasticCacheClient client = new AWSElasticCacheClient(
					new InetSocketAddress(2271));
			fail();
		} catch (IllegalStateException e) {
			assert (e.getMessage().contains("Invalid server"));
		} finally {
			configServer.stop();
		}
	}

	@Test
	public void testPollConfigAndUsage() throws Exception {
		TCPController configServer = new TCPController();
		int version = 10;
		configServer.setHandler(new MockHandler(version, serverList));
		configServer.setCodecFactory(new TextLineCodecFactory());
		configServer.bind(new InetSocketAddress(2271));

		try {
			AWSElasticCacheClient client = new AWSElasticCacheClient(
					new InetSocketAddress(2271));
			ClusterConfigration config = client.getCurrentConfig();
			assertEquals(config.getVersion(), version);
			assertEquals(addresses.size(), config.getNodeList().size());

			client.set("aws-cache", 0, "foobar");
			assertEquals("foobar", client.get("aws-cache"));
		} finally {
			configServer.stop();
		}
	}

	@Test
	public void testPollConfigInterval() throws Exception {
		TCPController cs1 = new TCPController();
		int version = 10;
		cs1.setHandler(new MockHandler(version, "localhost|localhost|2272"));
		cs1.setCodecFactory(new TextLineCodecFactory());
		cs1.bind(new InetSocketAddress(2271));
		TCPController cs2 = new TCPController();
		cs2.setHandler(new MockHandler(version + 1,
				"localhost|localhost|2271 localhost|localhost|2272"));
		cs2.setCodecFactory(new TextLineCodecFactory());
		cs2.bind(new InetSocketAddress(2272));

		try {
			AWSElasticCacheClient client = new AWSElasticCacheClient(
					new InetSocketAddress(2271), 3000);
			ClusterConfigration config = client.getCurrentConfig();
			assertEquals(config.getVersion(), version);
			assertEquals(1, config.getNodeList().size());
			assertEquals(2272, config.getNodeList().get(0).getPort());
			Thread.sleep(3500);
			config = client.getCurrentConfig();
			assertEquals(config.getVersion(), version + 1);
			assertEquals(2, config.getNodeList().size());
			assertEquals(2271, config.getNodeList().get(0).getPort());
			assertEquals(2272, config.getNodeList().get(1).getPort());
		} finally {
			cs1.stop();
			cs2.stop();
		}
	}
}
