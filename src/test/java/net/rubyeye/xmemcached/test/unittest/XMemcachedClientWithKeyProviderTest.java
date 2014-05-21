package net.rubyeye.xmemcached.test.unittest;

import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.KeyProvider;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedClientCallable;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class XMemcachedClientWithKeyProviderTest extends XMemcachedClientTest{
	
	private KeyProvider keyProvider;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		keyProvider = new KeyProvider() {
			
			public String process(String key) {
				// 现实中是基于某种规则进行字符串转换, 为了简单, 我直接用hashCode
				return String.valueOf(key.hashCode());
			}
		};
	}
	
	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(this.properties
						.getProperty("test.memcached.servers")));
		builder.setCommandFactory(new BinaryCommandFactory());
		ByteUtils.testing = true;
		return builder;
	}
	
	public void testKeyProvider(){
		String process = keyProvider.process("namespace:a");
		assertEquals("790852098", process);
	}
	
	public void testWithNamespaceAndKeyProvider() throws Exception{
		memcachedClient.setKeyProvider(keyProvider);
		memcachedClient.withNamespace("a", new MemcachedClientCallable<Void>() {

			public Void call(MemcachedClient client) throws MemcachedException, InterruptedException, TimeoutException {
				client.set("name", 0, "Mike Liu");
				return null;
			}
		});
		
		memcachedClient.invalidateNamespace("a");
		
		Object result = memcachedClient.withNamespace("a", new MemcachedClientCallable<Object>() {
			public Object call(MemcachedClient client) throws MemcachedException, InterruptedException, TimeoutException {
				return memcachedClient.get("name");
			}
		});
		
		assertNull(result);
	}
	
}
