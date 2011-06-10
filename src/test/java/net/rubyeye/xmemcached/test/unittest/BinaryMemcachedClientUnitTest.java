package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;

import org.junit.Ignore;
import org.junit.Test;

/**
 * 为了使Binary协议的测试通过，将检测key是否有非法字符，事实上binary协议无需检测的。
 * 
 * @author boyan
 * 
 */
public class BinaryMemcachedClientUnitTest extends XMemcachedClientTest {
	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(this.properties
						.getProperty("test.memcached.servers")));
		builder.setCommandFactory(new BinaryCommandFactory());
		ByteUtils.testing = true;
		return builder;
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
		builder.setCommandFactory(new BinaryCommandFactory());
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		ByteUtils.testing = true;
		return builder;
	}

	@Test
	public void testTouch() throws Exception {
		if (isMemcached1_6()) {
			assertNull(memcachedClient.get("name"));
			memcachedClient.set("name", 1, "dennis", new StringTranscoder(),
					1000);
			assertEquals("dennis", memcachedClient.get("name",
					new StringTranscoder()));

			// touch expiration to three seconds
			System.out.println(memcachedClient.touch("name", 3));
			Thread.sleep(2000);
			assertEquals("dennis", memcachedClient.get("name",
					new StringTranscoder()));
			Thread.sleep(1500);
			assertNull(memcachedClient.get("name"));
		}
	}

	@Test
	public void testTouchNotExists() throws Exception {
		if (isMemcached1_6()) {
			assertNull(memcachedClient.get("name"));
			assertFalse(memcachedClient.touch("name", 3));
		}
	}

	@Test
	public void testGetAndTouch_OneKey() throws Exception {
		if (isMemcached1_6()) {
			assertNull(memcachedClient.get("name"));
			memcachedClient.set("name", 1, "dennis", new StringTranscoder(),
					1000);
			assertEquals("dennis", memcachedClient.getAndTouch("name", 3));

			Thread.sleep(2000);
			assertEquals("dennis", memcachedClient.get("name",
					new StringTranscoder()));
			Thread.sleep(1500);
			assertNull(memcachedClient.get("name"));
		}
	}

	private boolean isMemcached1_6() throws Exception {
		Map<InetSocketAddress, String> versions = this.memcachedClient
				.getVersions();
		for (String v : versions.values()) {
			if (!v.startsWith("1.6")) {
				return false;
			}
		}
		return true;
	}

	@Test
	public void testGetAndTouch_NotExistsKey() throws Exception {
		if (isMemcached1_6()) {
			assertNull(memcachedClient.get("name"));
			assertNull(memcachedClient.getAndTouch("name", 3));
			assertNull(memcachedClient.getAndTouch("name", 3));
			assertNull(memcachedClient.get("name"));
		}
	}

	@Test
	@Ignore
	public void testBulkGetAndTouch() throws Exception {
		int count = 100;

		Map<String, Integer> keys = new HashMap<String, Integer>();
		int exp = 3;
		for (int i = 0; i < count; i++) {
			String key = String.valueOf(i);
			keys.put(key, exp);
			assertNull(this.memcachedClient.get(key));
		}
		for (int i = 0; i < count; i++) {
			this.memcachedClient.set(String.valueOf(i), i, i);
		}

		// Map<String, Integer> result = this.memcachedClient.getAndTouch(keys,
		// 5000L);
		// for (Map.Entry<String, Integer> entry : result.entrySet()) {
		// assertEquals(entry.getKey(), String.valueOf(entry.getValue()));
		// }

		Thread.sleep(3500);
		// assertTrue(this.memcachedClient.getAndTouch(keys, 5000L).isEmpty());

	}

}
