package net.rubyeye.xmemcached.test.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;

public class MultiServerExample {
	public static void main(String[] args) {
		try {
			String ip = "localhost";
			/**
			 * 采用一致性哈希算法
			 */
			MemcachedClientBuilder memcachedClientBuilder = new XMemcachedClientBuilder();
			memcachedClientBuilder
					.setSessionLocator(new KetamaMemcachedSessionLocator());
			MemcachedClient client = memcachedClientBuilder.build();
			client.addServer(ip, 12000);
			client.addServer(ip, 12001);

			System.out.println("begin test1");
			client.set("a1", 0, 1);
			client.set("a2", 0, 2);
			client.set("a3", 0, 3);
			client.set("a4", 0, 4);

			System.out.println(client.get("a1"));
			System.out.println(client.get("a2"));
			System.out.println(client.get("a3"));
			System.out.println(client.get("a4"));
			System.out.println("end test1");
			System.out.println("begin test2");
			// 测试批量取
			List<String> keys = new ArrayList<String>();
			keys.add("a1");
			keys.add("a2");
			keys.add("a3");
			keys.add("a4");

			Map<String, Object> result = client.get(keys);

			System.out.println(result.get("a1"));
			System.out.println(result.get("a2"));
			System.out.println(result.get("a3"));
			System.out.println(result.get("a4"));
			System.out.println("end test2");
			System.out.println("begin test3");
			client.set("a1", 0, new HashMap());
			client.set("a2", 0, new HashMap());
			client.set("a3", 0, new HashMap());
			client.set("a4", 0, new HashMap());

			System.out.println(client.get("a1"));
			System.out.println(client.get("a2"));
			System.out.println(client.get("a3"));
			System.out.println(client.get("a4"));
			System.out.println("end test3");
			System.out.println("begin test4");
			result = client.get(keys);

			System.out.println(result.get("a1"));
			System.out.println(result.get("a2"));
			System.out.println(result.get("a3"));
			System.out.println(result.get("a4"));
			System.out.println("end test4");
			client.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			// 超时
		} catch (InterruptedException e) {

		} catch (MemcachedException e) {
			e.printStackTrace();
			// 执行异常
		}
	}
}
