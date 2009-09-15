package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.KestrelCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

import com.google.code.yanf4j.util.ResourcesUtils;

public class KestrelClientUnitTest extends TestCase {
	static class UserDefinedClass implements Serializable {
		private String name;

		public UserDefinedClass(String name) {
			super();
			this.name = name;
		}

	}

	Properties properties;
	private MemcachedClient memcachedClient;

	@Override
	public void setUp() throws Exception {
		this.properties = ResourcesUtils
				.getResourceAsProperties("test.properties");
		MemcachedClientBuilder builder = newBuilder();
		this.memcachedClient = builder.build();
		this.memcachedClient.flushAll();
	}

	private MemcachedClientBuilder newBuilder() {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(this.properties
						.getProperty("test.kestrel.servers")));
		// Use kestrel command factory
		builder.setCommandFactory(new KestrelCommandFactory());
		return builder;
	}

	public void testPrimitiveAsString() throws Exception {
		this.memcachedClient.setPrimitiveAsString(true);
		// store integer
		for (int i = 0; i < 1000; i++) {
			this.memcachedClient.set("queue1", 0, i);
		}
		// but get string
		for (int i = 0; i < 1000; i++) {
			Assert.assertEquals(String.valueOf(i), this.memcachedClient
					.get("queue1"));
		}

		this.memcachedClient.setPrimitiveAsString(false);
		// store integer
		for (int i = 0; i < 1000; i++) {
			this.memcachedClient.set("queue1", 0, i);
		}
		// still get integer
		for (int i = 0; i < 1000; i++) {
			Assert.assertEquals(i, this.memcachedClient
					.get("queue1"));
		}
	}

	@Override
	public void tearDown() throws IOException {
		this.memcachedClient.shutdown();
	}

	public void testNormalSetAndGet() throws Exception {
		Assert.assertNull(this.memcachedClient.get("queue1"));

		Assert.assertTrue(this.memcachedClient.set("queue1", 0, "hello world"));
		Assert.assertEquals("hello world", this.memcachedClient.get("queue1"));

		Assert.assertNull(this.memcachedClient.get("queue1"));
	}

	public void testNormalSetAndGetMore() throws Exception {
		Assert.assertNull(this.memcachedClient.get("queue1"));
		for (int i = 0; i < 10; i++) {
			Assert.assertTrue(this.memcachedClient.set("queue1", 0, i));
		}
		for (int i = 0; i < 10; i++) {
			Assert.assertEquals(i, this.memcachedClient.get("queue1"));
		}
		Assert.assertNull(this.memcachedClient.get("queue1"));
	}

	public void testSetAndGetObject() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "a");
		map.put("b", "b");
		map.put("c", "c");
		Assert.assertTrue(this.memcachedClient.set("queue1", 0, map));

		Map<String, String> mapFromMQ = (Map<String, String>) this.memcachedClient
				.get("queue1");

		Assert.assertEquals(3, mapFromMQ.size());
		Assert.assertEquals("a", mapFromMQ.get("a"));
		Assert.assertEquals("b", mapFromMQ.get("b"));
		Assert.assertEquals("c", mapFromMQ.get("c"));
		Assert.assertNull(this.memcachedClient.get("queue1"));

		List<UserDefinedClass> userDefinedClassList = new ArrayList<UserDefinedClass>();
		userDefinedClassList.add(new UserDefinedClass("a"));
		userDefinedClassList.add(new UserDefinedClass("b"));
		userDefinedClassList.add(new UserDefinedClass("c"));

		Assert.assertTrue(this.memcachedClient.set("queue1", 0,
				userDefinedClassList));
		List<UserDefinedClass> userDefinedClassListFromMQ = (List<UserDefinedClass>) this.memcachedClient
				.get("queue1");

		Assert.assertEquals(3, userDefinedClassListFromMQ.size());

	}

	public void testBlockingFetch() throws Exception {
		this.memcachedClient.setOpTimeout(60000);
		long start = System.currentTimeMillis();
		// blocking read 1 second
		Assert.assertNull(this.memcachedClient.get("queue1/t=1000"));
		Assert.assertEquals(1000, System.currentTimeMillis() - start, 100);

		Assert.assertTrue(this.memcachedClient.set("queue1", 0, "hello world"));
		Assert.assertEquals("hello world", this.memcachedClient
				.get("queue1/t=1000"));

	}

	public void testReliableFetch() throws Exception {
		Assert.assertTrue(this.memcachedClient.set("queue1", 0, "hello world"));
		Assert.assertEquals("hello world", this.memcachedClient
				.get("queue1/open"));
		// close connection
		this.memcachedClient.shutdown();
		// still can fetch it
		MemcachedClient newClient = newBuilder().build();
		newClient.setOptimizeGet(false);
		// begin transaction
		Assert.assertEquals("hello world", newClient.get("queue1/open"));
		// confirm
		Assert.assertNull(newClient.get("queue1/close"));
		Assert.assertNull(newClient.get("queue1"));

		newClient.shutdown();
	}

	public void testPerformance() throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			this.memcachedClient.set("queue1", 0, "hello");
		}
		System.out.println("push 10000 message:"
				+ (System.currentTimeMillis() - start) + "ms");

		start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			this.memcachedClient.get("queue1");
		}
		System.out.println("fetch 10000 message:"
				+ (System.currentTimeMillis() - start) + "ms");
	}
}
