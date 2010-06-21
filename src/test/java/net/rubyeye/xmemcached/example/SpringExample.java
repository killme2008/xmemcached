package net.rubyeye.xmemcached.example;

import org.springframework.context.ApplicationContext;
import net.rubyeye.xmemcached.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringExample {
	public static void main(String[] args) throws Exception{
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"sampleApplicationContext.xml");

		MemcachedClient client1 = (MemcachedClient) ctx
				.getBean("memcachedClient1");
		MemcachedClient client2 = (MemcachedClient) ctx
		.getBean("memcachedClient2");
		test(client1);
		test(client2);
		client1.shutdown();
		client2.shutdown();

	}

	public static void test(MemcachedClient client) throws Exception {
		client.set("a", 0, 1);
		if ((Integer) client.get("a") != 1)
			System.err.println("get error");
		client.delete("a");
	}
}
