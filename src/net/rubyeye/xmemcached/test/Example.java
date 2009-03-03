package net.rubyeye.xmemcached.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.XMemcachedClient;

public class Example {
	public static void main(String[] args) {
		try {
			String ip = "192.168.222.100";

			int port = 11211;
			XMemcachedClient client = new XMemcachedClient(ip,
					port);
			// 存储操作
			if (!client.set("hello", 0, "dennis")) {
				System.err.println("set error");
			}
			client.add("hello", 0, "dennis");
			client.replace("hello", 0, "dennis");

			// get操作
			String name = (String) client.get("hello");

			// 批量获取
			List<String> keys = new ArrayList<String>();
			keys.add("hello");
			keys.add("test");
			Map<String, Object> map = client.get(keys);

			// delete操作
			if (!client.delete("hello", 1000)) {
				System.err.println("delete error");
			}

			// incr,decr
			client.incr("a", 4);
			client.decr("a", 4);

			// version
			String version = client.version();
            System.out.println(version);
			client.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
