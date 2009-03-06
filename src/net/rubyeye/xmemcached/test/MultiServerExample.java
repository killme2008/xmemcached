package net.rubyeye.xmemcached.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

public class MultiServerExample {
	public static void main(String[] args) {
		try {
			String ip = "192.168.222.100";

			XMemcachedClient client = new XMemcachedClient();
			client.addServer(ip, 11211);
			client.addServer(ip, 12000);
			client.addServer(ip, 12001);

			System.out.println("begin");
			client.set("a1", 0, 1);
			client.set("a2", 0, 2);
			client.set("a3", 0, 3);
			client.set("a4", 0, 4);

			System.out.println(client.get("a1"));
			System.out.println(client.get("a2"));
			System.out.println(client.get("a3"));
			System.out.println(client.get("a4"));

			client.set("a1", 0, new HashMap());
			client.set("a2", 0, new HashMap());
			client.set("a3", 0, new HashMap());
			client.set("a4", 0, new HashMap());
			System.out.println("end");
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
