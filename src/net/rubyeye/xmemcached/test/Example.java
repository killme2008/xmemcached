package net.rubyeye.xmemcached.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import net.rubyeye.xmemcached.XMemcachedClient;

class Name implements Serializable {
	String firstName;
	String lastName;
	int age;
	int money;

	public Name(String firstName, String lastName, int age, int money) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.age = age;
		this.money = money;
	}

	public String toString() {
		return "[" + firstName + " " + lastName + ",age=" + age + ",money="
				+ money + "]";
	}

}

public class Example {
	public static void main(String[] args) {
		try {
			String ip = "192.168.222.100";

			int port = 11211;
			XMemcachedClient client = new XMemcachedClient(ip, port);
			// 存储操作
			if (!client.set("hello", 0, "dennis")) {
				System.err.println("set error");
			}
			client.add("hello", 0, "dennis");
			client.replace("hello", 0, "dennis");

			// get操作
			String name = (String) client.get("hello");
			System.out.println(name);

			// 批量获取
			List<String> keys = new ArrayList<String>();
			keys.add("hello");
			keys.add("test");
			Map<String, Object> map = client.get(keys);
			System.out.println("map size:"+map.size());

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
			// 增删改查自定义对象
			Name dennis = new Name("dennis", "zhuang", 26, -1);
			System.out.println("dennis:" + dennis);
			client.set("dennis", 0, dennis);

			Name cachedPerson = (Name) client.get("dennis");
			System.out.println("cachedPerson:" + cachedPerson);
			cachedPerson.money = -10000;

			client.replace("dennis", 0, cachedPerson);
			Name cachedPerson2 = (Name) client.get("dennis");
			System.out.println("cachedPerson2:" + cachedPerson2);

			// delete
			client.delete("dennis");
			System.out.println("after delete:" + client.get("dennis"));
			client.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
