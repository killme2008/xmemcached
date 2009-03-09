package net.rubyeye.xmemcached.test;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;

/**
 * 测试节点的添加和删除对缓存命中率的影响
 * 
 * @author Administrator
 * 
 */
public class CacheHitRateTest {
	static int NUM = 1000;

	public static void main(String[] args) throws Exception {
		String ip = "192.168.222.100";
		XMemcachedClient client = new XMemcachedClient(new KetamaMemcachedSessionLocator(HashAlgorithm.CRC32_HASH));
		client.addServer(ip, 12000);
		client.addServer(ip, 12001);
		//client.addServer(ip, 11211);
		//client.addServer(ip, 12003);
		//client.addServer(ip, 12004);
		//init(client);
		calcHitRate(client);
		client.shutdown();
	}

	public static void init(XMemcachedClient client) throws Exception {
		for (int i = 0; i < NUM; i++)
			client.set(String.valueOf(i), 0, i);
	}

	public static void calcHitRate(XMemcachedClient client) throws Exception {
		int hits = 0;
		int misses = 0;
		for (int i = 0; i < NUM; i++) {
			if (client.get(String.valueOf(i)) == null)
				misses++;
			else
				hits++;
		}

		System.out.println("hit rate:" + ((double) hits) / NUM * 100 + "%");

	}

}
