package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class StandardHashMemcachedClientTest extends XMemcachedClientTest {

	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(this.properties
						.getProperty("test.memcached.servers")));
		//builder.setConnectionPoolSize(Runtime.getRuntime().availableProcessors());
		return builder;
	}
	
	public void testStoreNoReply() throws Exception {
		memcachedClient.replaceWithNoReply("name", 0, 1);
		assertNull(memcachedClient.get("name"));

		memcachedClient.setWithNoReply("name", 1, "dennis",
				new StringTranscoder());
		assertEquals("dennis", memcachedClient.get("name"));
		Thread.sleep(2000);
		assertNull(memcachedClient.get("name"));

		memcachedClient.setWithNoReply("name", 0, "dennis",
				new StringTranscoder());
		memcachedClient.appendWithNoReply("name", " zhuang");
		memcachedClient.prependWithNoReply("name", "hello ");
		assertEquals("hello dennis zhuang", memcachedClient.get("name"));

		memcachedClient.addWithNoReply("name", 0, "test",
				new StringTranscoder());
		assertEquals("hello dennis zhuang", memcachedClient.get("name"));
		memcachedClient.replaceWithNoReply("name", 0, "test",
				new StringTranscoder());
		assertEquals("test", memcachedClient.get("name"));

		memcachedClient.setWithNoReply("a", 0, 1);
		GetsResponse<Integer> getsResponse = memcachedClient.gets("a");
		memcachedClient.casWithNoReply("a", 0, getsResponse,
				new CASOperation<Integer>() {

					public int getMaxTries() {
						return 100;
					}

					public Integer getNewValue(long currentCAS,
							Integer currentValue) {
						return currentValue + 1;
					}

				});
		assertEquals(2, memcachedClient.get("a"));
		// repeat onece,it is not effected,because cas value is changed
		memcachedClient.casWithNoReply("a", getsResponse,
				new CASOperation<Integer>() {

					public int getMaxTries() {
						return 1;
					}

					public Integer getNewValue(long currentCAS,
							Integer currentValue) {
						return currentValue + 1;
					}

				});
		assertEquals(2, memcachedClient.get("a"));

		memcachedClient.casWithNoReply("a", new CASOperation<Integer>() {

			public int getMaxTries() {
				return 1;
			}

			public Integer getNewValue(long currentCAS, Integer currentValue) {
				return currentValue + 1;
			}

		});
		assertEquals(3, memcachedClient.get("a"));
	}


	public void testDeleteWithNoReply() throws Exception {
		assertTrue(memcachedClient.set("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name"));
		memcachedClient.deleteWithNoReply("name");
		assertNull(memcachedClient.get("name"));
		memcachedClient.deleteWithNoReply("not_exists");

		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		memcachedClient.deleteWithNoReply("name");
		assertNull(memcachedClient.get("name"));
		// add,replace success
		assertTrue(memcachedClient.add("name", 0, "zhuang"));
		assertTrue(memcachedClient.replace("name", 0, "zhuang"));
	}
	
	public void testFlushAllWithNoReply() throws Exception {
		for (int i = 0; i < 10; i++) {
			assertTrue(memcachedClient.add(String.valueOf(i), 0, i));
		}
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 20; i++) {
			keys.add(String.valueOf(i));
		}
		Map<String, Integer> result = memcachedClient.get(keys);
		assertEquals(10, result.size());
		for (int i = 0; i < 10; i++) {
			assertEquals((Integer) i, result.get(String.valueOf(i)));
		}
		memcachedClient.flushAllWithNoReply();
		result = memcachedClient.get(keys, 5000);
		assertTrue(result.isEmpty());
	}

	public void testIncrWithNoReply() throws Exception {
		memcachedClient.incrWithNoReply("a", 5);
		assertTrue(memcachedClient.set("a", 0, "1"));
		memcachedClient.incrWithNoReply("a", 5);
		assertEquals("6", memcachedClient.get("a"));
		memcachedClient.incrWithNoReply("a", 4);
		assertEquals("10", memcachedClient.get("a"));
	}

	public void testDecrWithNoReply() throws Exception {
		memcachedClient.decrWithNoReply("a", 5);

		assertTrue(memcachedClient.set("a", 0, "100"));
		memcachedClient.decrWithNoReply("a", 50);
		assertEquals("50", memcachedClient.get("a"));
		memcachedClient.decrWithNoReply("a", 4);
		assertEquals("46", memcachedClient.get("a"));
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
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		return builder;
	}

}
