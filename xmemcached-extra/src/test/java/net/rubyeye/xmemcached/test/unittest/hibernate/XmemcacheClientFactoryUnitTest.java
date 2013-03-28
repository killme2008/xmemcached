package net.rubyeye.xmemcached.test.unittest.hibernate;

import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.utils.hibernate.XmemcachedClientFactory;

import com.google.code.yanf4j.util.ResourcesUtils;
import com.googlecode.hibernate.memcached.Memcache;
import com.googlecode.hibernate.memcached.PropertiesHelper;
import com.googlecode.hibernate.memcached.spymemcached.SpyMemcacheClientFactory;

public class XmemcacheClientFactoryUnitTest extends TestCase {
	Properties properties;

	@Override
	public void setUp() throws Exception {
		Properties testProperties = ResourcesUtils
				.getResourceAsProperties("test.properties");

		this.properties = new Properties();
		this.properties.put(XmemcachedClientFactory.PROP_COMMAND_FACTORY,
				"TextCommandFactory");
		this.properties.put(XmemcachedClientFactory.PROP_SESSION_LOCATOR,
				"KetamaMemcachedSessionLocator");
		this.properties.put(XmemcachedClientFactory.PROP_HASH_ALGORITHM,
				"CRC32_HASH");
		this.properties.put(XmemcachedClientFactory.PROP_OPERATION_TIMEOUT,
				2000);
		this.properties.put(XmemcachedClientFactory.PROP_READ_BUFFER_SIZE,
				4 * 1024);
		this.properties.put(XmemcachedClientFactory.PROP_SERVERS,
				testProperties.get("test.memcached.servers"));

	}

	public void testXmemcachedClient() throws Exception {
		PropertiesHelper propertiesHelper = new PropertiesHelper(
				this.properties);
		XmemcachedClientFactory clientFactory = new XmemcachedClientFactory(
				propertiesHelper);
		Memcache cache = clientFactory.createMemcacheClient();
		testCache(cache);
		cache.shutdown();
	}

	private void testCache(Memcache cache) {
		cache.set("a", 0, 1);
		assertEquals(1, cache.get("a"));
		cache.delete("a");
		assertNull(cache.get("a"));
		cache.set("a", 0, 1);
		cache.set("b", 0, 2);

		Map<String, Object> map = cache.getMulti("a", "b");
		assertEquals(2, map.size());
		assertEquals(1, map.get("a"));
		assertEquals(2, map.get("b"));

		cache.incr("c", 1, 10);
		assertEquals("10", cache.get("c"));
		cache.incr("c", 1, 0);
		assertEquals("11", cache.get("c"));
		cache.delete("c");
		assertNull(cache.get("c"));
	}
	
	public void testSpymemcacheClient() throws Exception {
		PropertiesHelper propertiesHelper = new PropertiesHelper(
				this.properties);
		SpyMemcacheClientFactory clientFactory = new SpyMemcacheClientFactory(
				propertiesHelper);
		Memcache cache = clientFactory.createMemcacheClient();
		testCache(cache);
		cache.shutdown();
	}
}
