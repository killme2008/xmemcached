package net.rubyeye.xmemcached.test.unittest;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class WeightedServerMemcachedClientTest extends XMemcachedClientTest {

	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {
		return createWeightedBuilder();
	}

	@Override
	public MemcachedClientBuilder createWeightedBuilder() throws Exception {
		MemcachedClientBuilder builder = XMemcachedClientBuilder
				.newMemcachedClientBuilder(AddrUtil
						.getAddressesWithWeight(properties
								.getProperty("test.memcached.weighted.servers")));
		return builder;
	}

}
