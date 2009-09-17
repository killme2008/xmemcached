package net.rubyeye.xmemcached.example;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import java.util.*;

public class KestrelExample {
	public static void main(String[] args) throws Exception {
		XMemcachedClientBuilder builder = new XMemcachedClientBuilder();
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		MemcachedClient xmc = builder.build();
		xmc.addServer("localhost", 22133);
		xmc.setOpTimeout(60000);

		System.out.println(xmc.get("test_queue/t=250"));
		xmc.set("test_queue", 0, "test");
		System.out.println(xmc.get("test_queue"));// 正常
		xmc.set("test_queue", 200, new ArrayList());
		xmc.get("test_queue/open");
		xmc.get("test_queue/close");
		xmc.shutdown();
	}
}
