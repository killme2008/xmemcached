package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.GetsResponse;
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
public class BinaryMemcachedClientIT extends XMemcachedClientIT {
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

	@Override
	@Test
	public void testTouch() throws Exception {
		if (this.isMemcached1_6()) {
			assertNull(this.memcachedClient.get("name"));
			this.memcachedClient.set("name", 1, "dennis", new StringTranscoder(),
					1000);
			assertEquals("dennis", this.memcachedClient.get("name",
					new StringTranscoder()));

			// touch expiration to three seconds
			System.out.println(this.memcachedClient.touch("name", 3));
			Thread.sleep(2000);
			assertEquals("dennis", this.memcachedClient.get("name",
					new StringTranscoder()));
			Thread.sleep(1500);
			assertNull(this.memcachedClient.get("name"));
		}
	}

	@Test
	public void testTouchNotExists() throws Exception {
		if (this.isMemcached1_6()) {
			assertNull(this.memcachedClient.get("name"));
			assertFalse(this.memcachedClient.touch("name", 3));
		}
	}

	@Test
	public void testGetAndTouch_OneKey() throws Exception {
		if (this.isMemcached1_6()) {
			assertNull(this.memcachedClient.get("name"));
			this.memcachedClient.set("name", 1, "dennis", new StringTranscoder(),
					1000);
			assertEquals("dennis", this.memcachedClient.getAndTouch("name", 3));

			Thread.sleep(2000);
			assertEquals("dennis", this.memcachedClient.get("name",
					new StringTranscoder()));
			Thread.sleep(1500);
			assertNull(this.memcachedClient.get("name"));
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
		if (this.isMemcached1_6()) {
			assertNull(this.memcachedClient.get("name"));
			assertNull(this.memcachedClient.getAndTouch("name", 3));
			assertNull(this.memcachedClient.getAndTouch("name", 3));
			assertNull(this.memcachedClient.get("name"));
		}
	}

	@Test
	public void testDeleteWithCAS()throws Exception{
		this.memcachedClient.set("a", 0, 1);
		GetsResponse<Integer> gets = this.memcachedClient.gets("a");
		this.memcachedClient.set("a", 0, 2);
		assertFalse(this.memcachedClient.delete("a", gets.getCas(), 1000));
		assertEquals(2, this.memcachedClient.get("a"));
		gets = this.memcachedClient.gets("a");
		assertTrue(this.memcachedClient.delete("a", gets.getCas(), 1000));
		assertNull(this.memcachedClient.get("a"));
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
