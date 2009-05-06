/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.io.IOException;
import java.io.Serializable;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.IntegerTranscoder;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

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

			XMemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses("192.168.207.101:12000"));
			XMemcachedClient client = builder.build();
			if (!client.set("hello", 0, "dennis")) {
				System.err.println("set error");
			}
			client.add("hello", 0, "dennis");
			client.replace("hello", 0, "dennis");
			client.append("hello", " good");
			client.prepend("hello", "hello ");
			String name = client.get("hello", new StringTranscoder());
			System.out.println(name);

			List<String> keys = new ArrayList<String>();
			keys.add("hello");
			keys.add("test");
			Map<String, Integer> map = client
					.get(keys, new IntegerTranscoder());
			System.out.println("map size:" + map.size());

			if (!client.delete("hello", 1000)) {
				System.err.println("delete error");
			}

			client.set("a", 0, "4");
			System.out.println(client.incr("a", 4));
			System.out.println(client.decr("a", 4));

			String version = client.version();
			System.out.println("memcached version:" + version);
			Name dennis = new Name("dennis", "zhuang", 26, -1);
			System.out.println("dennis:" + dennis);
			client.set("dennis", 0, dennis);

			Name cachedPerson = (Name) client.get("dennis");
			System.out.println("cachedPerson:" + cachedPerson);
			cachedPerson.money = -10000;

			client.replace("dennis", 0, cachedPerson);
			Name cachedPerson2 = (Name) client.get("dennis");
			System.out.println("cachedPerson2:" + cachedPerson2);

			client.delete("dennis");
			System.out.println("after delete:" + client.get("dennis"));

			map = new HashMap<String, Integer>();
			for (int i = 0; i < 1000; i++) {
				map.put(String.valueOf(i), i);
			}
			if (!client.set("map", 0, map, 10000)) {
				System.err.println("set map error");
			}
			HashMap<String, Integer> cachedMap = (HashMap<String, Integer>) client
					.get("map");
			if (cachedMap.size() != 1000) {
				System.err.println("get map error");
			}
			for (Object key : cachedMap.keySet()) {
				if (!cachedMap.get(key).equals(Integer.parseInt((String) key))) {
					System.err.println("get map error");
				}
			}
			for (int i = 0; i < 100; i++) {
				if (client.get("hello__" + i) != null) {
					System.err.println("get error");
				}
			}
			for (int i = 0; i < 100; i++) {
				if (client.delete("hello__" + i)) {
					System.err.println("get error");
				}
			}

			long start = System.currentTimeMillis();
			for (int i = 0; i < 200; i++) {
				if (!client.set("test", 0, i)) {
					System.out.println("set error");
				}
			}
			System.out.println(System.currentTimeMillis() - start);
			client.delete("test");
			// 测试cas
			client.set("a", 0, 1);
			GetsResponse<Integer> result = client.gets("a");
			long cas = result.getCas();
			if (result.getValue() != 1) {
				System.err.println("gets error");
			}
			System.out.println("cas value:" + cas);
			if (!client.cas("a", 0, 2, cas)) {
				System.err.println("cas error");
			}
			result = client.gets("a");

			if (result.getValue() != 2) {
				System.err.println("cas error");
			}
			List<String> getsKeys = new ArrayList<String>();
			getsKeys.add("a");
			Map<String, GetsResponse<Integer>> getsMap = client.gets(getsKeys);
			System.out.println("getsMap:" + getsMap.toString());

			/**
			 * 合并gets和cas，利用CASOperation
			 */
			client.cas("a", 0, new CASOperation<Integer>() {

				@Override
				public int getMaxTries() {
					return 1;
				}

				@Override
				public Integer getNewValue(long currentCAS, Integer currentValue) {
					System.out.println("current value " + currentValue);
					return 3;
				}
			});
			result = client.gets("a");
			if (result.getValue() != 3) {
				System.err.println("cas error");
			}
			result.setCas(100);// 改变cas值，因此需要试2次
			client.cas("a", result, new CASOperation<Integer>() {

				@Override
				public int getMaxTries() {
					return 2;
				}

				@Override
				public Integer getNewValue(long currentCAS, Integer currentValue) {
					System.out.println("current value " + currentValue);
					return 4;
				}
			});
			result = client.gets("a");
			if (result.getValue() != 4) {
				System.err.println("cas error");
			}
			keys.add("a");
			// 批量gets
			System.out.println(client.gets(keys).get("a").getValue());
			client.flushAll(); // 使所有数据项失效
			// 查看统计信息
			System.out.println(client.stats("192.168.207.101:12000", 1000)); // 查看统计信息
			client.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
			// 超时
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (MemcachedException e) {
			e.printStackTrace();
			// 执行异常
		}

	}
}
