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
package net.rubyeye.xmemcached.example;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
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

	@Override
	public String toString() {
		return "[" + this.firstName + " " + this.lastName + ",age=" + this.age
				+ ",money=" + this.money + "]";
	}
}

public class Example {

	public static void main(String[] args) {
		try {

			if (args.length < 1) {
				System.err.println("Useage:java Example [servers]");
				System.exit(1);
			}

			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(args[0]));
			builder.setCommandFactory(new BinaryCommandFactory());
			MemcachedClient client = builder.build();
			if (!client.set("hello", 0, "world")) {
				System.err.println("set error");
			}
			if (client.add("hello", 0, "dennis")) {
				System.err.println("Add error,key is existed");
			}
			if (!client.replace("hello", 0, "dennis")) {
				System.err.println("replace error");
			}
			client.append("hello", " good");
			client.prepend("hello", "hello ");
			String name = client.get("hello", new StringTranscoder());
			System.out.println(name);
			// if (!client.delete("hello")) {
			// System.err.println("delete error");
			// }
			client.deleteWithNoReply("hello");
			System.out.println(client.get("hello"));

			System.out.println(client.getVersions());
			System.out.println(client.getStatsByItem("items"));

			client.set("a", 0, 1);
			client.set("b", 0, 2);

			client.set("c", 0, 3);
			client.setWithNoReply("d", 0, 4);
			java.util.List<String> list = new ArrayList<String>();
			list.add("a");
			list.add("b");
			list.add("c");
			list.add("d");
			System.out.println(client.get(list));
			System.out.println(client.gets(list));
			System.out.println(client.gets("a"));

			client.flushAll();

			System.out.println("After flush all");
			System.out.println(client.get(list));
			System.out.println(client.get("a"));
			client.addWithNoReply("a", 0, 4);
			System.out.println(client.gets("a"));

			client.flushAllWithNoReply();
			System.out.println("After flush all2");
			System.out.println(client.gets("a"));

			System.out.println(client.incr("a", 4));
			System.out.println(client.incr("a", 4));
			System.out.println(client.incr("a", 5));
			System.out.println(client.incr("a", 6));
			client.incrWithNoReply("a", 10);
			System.out.println(client.get("a"));
			System.out.println(client.decr("a", 15));
			System.out.println(client.decr("a", 5));
			client.decr("a", 5);
			System.out.println(client.get("a"));

			// 测试CAS
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
			client.flushAll(); // 使所有数据项失效
			// 查看统计信息
			System.out.println(client.getStats()); // 查看统计信息

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
