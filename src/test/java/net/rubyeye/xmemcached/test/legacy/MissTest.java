package net.rubyeye.xmemcached.test.legacy;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;

public class MissTest {
	public static void main(String[] args) throws Exception {
		MemcachedClient mc = new XMemcachedClient();
		mc.addServer("localhost", 12000);
		mc.addServer("localhost", 12001);
		for (int i = 0; i < 10000; i++)
			mc.delete(String.valueOf(i));
		for (int i = 0; i < 10000; i++)
			if (i % 2 == 0)
				mc.set(String.valueOf(i), 0, i);
		for (int i = 0; i < 10000; i++) {
			if (i % 2 == 0) {
				if ((Integer) mc.get(String.valueOf(i)) != i)
					System.err.println("miss test fail1");
			} else {
				if (mc.get(String.valueOf(i)) != null)
					System.err.println("miss test fail2");
			}
		}
		System.out.println("test done!");
		mc.shutdown();
	}
}
