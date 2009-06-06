package net.rubyeye.xmemcached.test.unittest;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class ConsistentHashMemcachedClientTest extends XMemcachedClientTest{
	@Override
	public MemcachedClientBuilder createBuilder()throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(properties.getProperty("test.memcached.servers")));
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		
		return builder;
	}
}
