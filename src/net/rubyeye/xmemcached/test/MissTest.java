package net.rubyeye.xmemcached.test;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;

public class MissTest {
	public static void main(String[] args) throws Exception {
		MemcachedClient mc = new XMemcachedClient();
		mc.addServer("192.168.207.101", 12000);
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
