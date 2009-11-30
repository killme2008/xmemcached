package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.List;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.Protocol;

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
		ByteUtils.testing=true;
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
		ByteUtils.testing=true;
		return builder;
	}
}
