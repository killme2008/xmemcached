package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.List;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class ConsistentHashMemcachedClientTest extends StandardHashMemcachedClientTest {
	@Override
	public MemcachedClientBuilder createBuilder() throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(properties.getProperty("test.memcached.servers")));
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());

		return builder;
	}

	@Override
	public MemcachedClientBuilder createWeightedBuilder() throws Exception {
		List<InetSocketAddress> addressList = AddrUtil.getAddresses(properties
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
