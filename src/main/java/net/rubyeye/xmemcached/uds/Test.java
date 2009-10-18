package net.rubyeye.xmemcached.uds;

import java.net.InetSocketAddress;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;

public class Test {
	public static void main(String[] args) throws Exception {
		MemcachedClient client = new XMemcachedClient(new UDSocketAddress(
				"/home/dennis/memcached"));
		//MemcachedClient client=new XMemcachedClient(new InetSocketAddress("localhost",12000));
		int num = 10000;
		long start = System.currentTimeMillis();
		for (int i = 0; i < num; i++)
			if(!client.set("hello" + i, 0, "hello" + i))
				throw new RuntimeException();
		System.out.println(System.currentTimeMillis() - start);
	}
}
