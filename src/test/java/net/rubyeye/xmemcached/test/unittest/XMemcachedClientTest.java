package net.rubyeye.xmemcached.test.unittest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import com.google.code.yanf4j.util.ResourcesUtils;
import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import junit.framework.TestCase;

public class XMemcachedClientTest extends TestCase {
	MemcachedClient memcachedClient;

	public void setUp() throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("test.properties");
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(properties.getProperty("test.memcached.servers")));
		memcachedClient = builder.build();
		memcachedClient.flushAll(5000);
	}

	public void testGet() throws Exception {
		try {
			assertNull(memcachedClient.get("name"));
		} catch (Exception e) {

		}
		memcachedClient.set("name", 1, "dennis", new StringTranscoder(), 1000);
		assertEquals("dennis", memcachedClient.get("name",new StringTranscoder()));
		Thread.sleep(2000);
		// expire
		assertNull(memcachedClient.get("name"));
		// blank key
		try {
			memcachedClient.get("");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Key must not be blank", e.getMessage());
		}
		// null key
		try {
			memcachedClient.get((String) null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Key must not be blank", e.getMessage());
		}
		// invalid key
		try {
			memcachedClient.get("test\r\n");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith(
					"Key contains invalid characters"));
		}
		try {
			memcachedClient.get("test test2");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith(
					"Key contains invalid characters"));
		}
		// key is too long
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 256; i++)
				sb.append(i);
			memcachedClient.get(sb.toString());
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Key is too long (maxlen = 250)", e.getMessage());
		}
		// client is shutdown
		try {
			memcachedClient.shutdown();
			memcachedClient.get("name");
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Xmemcached is stopped", e.getMessage());
		}
	}

	public void testStore() throws Exception {
		// add,replace
		assertTrue(memcachedClient.add("name", 0, "dennis"));
		assertFalse(memcachedClient.add("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name", 2000));

		assertFalse(memcachedClient.replace("unknownKey", 0, "test"));
		assertTrue(memcachedClient.replace("name", 1, "zhuang"));
		assertEquals("zhuang", memcachedClient.get("name", 2000));
		Thread.sleep(2000);
		assertNull(memcachedClient.get("zhuang"));
		// set
		assertTrue(memcachedClient.set("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name", 2000));

		assertTrue(memcachedClient.set("name", 1, "zhuang"));
		assertEquals("zhuang", memcachedClient.get("name", 2000));
		Thread.sleep(2000);
		assertNull(memcachedClient.get("zhuang"));
		
		//append,prepend
		assertTrue(memcachedClient.set("name",0, "dennis"));
		assertTrue(memcachedClient.prepend("name","hello "));
		assertEquals("hello dennis",memcachedClient.get("name"));
		assertTrue(memcachedClient.append("name", " zhuang"));
		assertEquals("hello dennis zhuang",memcachedClient.get("name"));
		memcachedClient.delete("name");
		assertFalse(memcachedClient.prepend("name","hello "));
		assertFalse(memcachedClient.append("name", " zhuang"));
		
		// store list
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < 100; i++)
			list.add(String.valueOf(i));
		assertTrue(memcachedClient.add("list", 0, list));
		List<String> result = memcachedClient.get("list");
		assertEquals(100, result.size());

		for (int i = 0; i < result.size(); i++) {
			assertEquals(list.get(i), result.get(i));
		}
	}

	public void testDelete() throws Exception {
		assertTrue(memcachedClient.set("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name"));
		assertTrue(memcachedClient.delete("name"));
		assertNull(memcachedClient.get("name"));
		assertFalse(memcachedClient.delete("not_exists"));

		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		assertTrue(memcachedClient.delete("name", 2));
		assertNull(memcachedClient.get("name"));
		// add,replace fail
		assertFalse(memcachedClient.add("name", 0, "zhuang"));
		assertFalse(memcachedClient.replace("name", 0, "zhuang"));
		Thread.sleep(3000);
		//add,replace success
		assertTrue(memcachedClient.add("name", 0, "zhuang"));
		assertTrue(memcachedClient.replace("name", 0, "zhuang"));
	}

	//
	public void testMultiGet() throws Exception {
		for (int i = 0; i < 50; i++)
			assertTrue(memcachedClient.add(String.valueOf(i), 0, i));

		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100; i++)
			keys.add(String.valueOf(i));

		Map<String, Integer> result = memcachedClient.get(keys, 10000);
		assertEquals(50, result.size());

		for (int i = 0; i < 50; i++)
			assertEquals((Integer) i, result.get(String.valueOf(i)));

	}

	public void testGets() throws Exception {
		memcachedClient.add("name", 0, "dennis");
		GetsResponse<String> getsResponse = memcachedClient.gets("name");
		assertEquals("dennis", getsResponse.getValue());
		long oldCas = getsResponse.getCas();
		getsResponse = memcachedClient.gets("name");
		assertEquals("dennis", getsResponse.getValue());
		assertEquals(oldCas, getsResponse.getCas());

		memcachedClient.set("name", 0, "zhuang");
		getsResponse = memcachedClient.gets("name");
		assertEquals("zhuang", getsResponse.getValue());
		assertFalse(oldCas == getsResponse.getCas());

	}

	public void testVersion() throws Exception {
		assertNotNull(memcachedClient.version());
		System.out.println(memcachedClient.version());
		assertTrue(memcachedClient.getVersions(5000).size()>0);
		System.out.println(memcachedClient.getVersions());

	}

	public void testStats() throws Exception {
		assertTrue(memcachedClient.getStats().size() > 0);
		System.out.println(memcachedClient.getStats());
		memcachedClient.set("a", 0, 1);
		assertTrue(memcachedClient.getStatsByItem("items").size() > 0);
		System.out.println(memcachedClient.getStatsByItem("items"));
	}

	public void testFlushAll() throws Exception {
		for (int i = 0; i < 50; i++)
			assertTrue(memcachedClient.add(String.valueOf(i), 0, i));
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100; i++)
			keys.add(String.valueOf(i));
		Map<String, Integer> result = memcachedClient.get(keys);
		assertEquals(50, result.size());
		for (int i = 0; i < 50; i++)
			assertEquals((Integer) i, result.get(String.valueOf(i)));
		memcachedClient.flushAll();
		result = memcachedClient.get(keys);
		assertTrue(result.isEmpty());
	}

	public void testIncr() throws Exception {
		try {
			memcachedClient.incr("a", 5);
			fail();
		} catch (MemcachedException e) {
			assertEquals(
					"net.rubyeye.xmemcached.exception.MemcachedException: The key's value is not found for increase or decrease",
					e.getMessage());
		}
		assertTrue(memcachedClient.set("a", 0, "1"));
		assertEquals(6, memcachedClient.incr("a", 5));
		assertEquals(10, memcachedClient.incr("a", 4));
	}

	public void testDecr() throws Exception {
		try {
			memcachedClient.decr("a", 5);
			fail();
		} catch (MemcachedException e) {
			assertEquals(
					"net.rubyeye.xmemcached.exception.MemcachedException: The key's value is not found for increase or decrease",
					e.getMessage());
		}
		assertTrue(memcachedClient.set("a", 0, "100"));
		assertEquals(50, memcachedClient.decr("a", 50));
		assertEquals(46, memcachedClient.decr("a", 4));
	}

	public void testCAS() throws Exception {
		memcachedClient.add("name", 0, "dennis");
		GetsResponse<String> getsResponse = memcachedClient.gets("name");
		assertEquals("dennis", getsResponse.getValue());
		assertTrue(memcachedClient.cas("name", getsResponse,
				new CASOperation<String>() {

					@Override
					public int getMaxTries() {
						return 1;
					}

					@Override
					public String getNewValue(long currentCAS,
							String currentValue) {
						return "zhuang";
					}

				}));
		assertEquals("zhuang", memcachedClient.get("name"));
		getsResponse = memcachedClient.gets("name");
		memcachedClient.set("name", 0, "dennis");
		// cas fail
		assertFalse(memcachedClient.cas("name", 0, "zhuang", getsResponse
				.getCas()));
		assertEquals("dennis", memcachedClient.get("name"));
	}

	public void tearDown() throws Exception {
		this.memcachedClient.shutdown();
	}
}
