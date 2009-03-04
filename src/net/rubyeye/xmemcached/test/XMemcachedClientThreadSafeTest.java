package net.rubyeye.xmemcached.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import net.rubyeye.xmemcached.XMemcachedClient;

class TestThread implements Runnable {
	private static final int ELEMENT_NUM = 500;
	XMemcachedClient xmemcachedClient;
	CyclicBarrier barrier;

	static List<Integer> list = new ArrayList<Integer>();
	static Map<String, Integer> map = new HashMap<String, Integer>();
	static Map<String, NameClass> map2 = new HashMap<String, NameClass>();

	static {
		for (int i = 0; i < ELEMENT_NUM; i++)
			list.add(i);
		for (int i = 0; i < ELEMENT_NUM; i++)
			map.put(String.valueOf(i), i);
		for (int i = 0; i < ELEMENT_NUM; i++)
			map2.put(String.valueOf(i), new NameClass(String.valueOf(i), String
					.valueOf(i)));
	}

	int number;

	final static int NUM = 1000;

	public TestThread(int number, XMemcachedClient xmemcachedClient,
			CyclicBarrier barrier) {
		this.xmemcachedClient = xmemcachedClient;
		this.number = number;
		this.barrier = barrier;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			barrier.await();
			// 增删简单类型
			for (int i = 0; i < NUM; i++) {
				assert (this.xmemcachedClient.set("test_" + number + "_" + i,
						0, i, 2000));
			}
			for (int i = 0; i < NUM; i++) {
				assert ((Integer) this.xmemcachedClient.get("test_" + number
						+ "_" + i) == i);
			}
			for (int i = 0; i < NUM; i++) {
				assert (this.xmemcachedClient
						.delete("test_" + number + "_" + i));
			}
			for (int i = 0; i < NUM; i++) {
				assert ((Integer) this.xmemcachedClient.get("test_" + number
						+ "_" + i) == null);
			}
			assert (xmemcachedClient.set("list_" + number, 0, list));
			List<Integer> cachedList = (List<Integer>) xmemcachedClient
					.get("list_" + number);
			assert (cachedList.size() == ELEMENT_NUM);
			for (int i = 0; i < ELEMENT_NUM; i++)
				assert (cachedList.get(i) == i);
			assert (xmemcachedClient.delete("list_" + number));

			assert (xmemcachedClient.set("map_" + number, 0, map));
			Map<String, Integer> cachedMap = (Map<String, Integer>) xmemcachedClient
					.get("map_" + number);
			assert (cachedMap.size() == ELEMENT_NUM);
			for (int i = 0; i < ELEMENT_NUM; i++)
				assert (cachedMap.get(String.valueOf(i)) == i);
			assert (xmemcachedClient.delete("map_" + number));

			assert (xmemcachedClient.set("map2_" + number, 0, map2));
			Map<String, NameClass> cachedMap2 = (Map<String, NameClass>) xmemcachedClient
					.get("map2_" + number);
			assert (cachedMap2.size() == ELEMENT_NUM);
			for (int i = 0; i < ELEMENT_NUM; i++)
				assert (((NameClass) cachedMap2.get(String.valueOf(i))).firstName
						.equals(String.valueOf(i)));
			assert (xmemcachedClient.delete("map2_" + number));
			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(this.number + " " + e.getMessage());
		}
	}
}

public class XMemcachedClientThreadSafeTest {
	static int num = 500;

	public static void main(String args[]) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		String ip = "192.168.222.100";
		int port = 11211;
		XMemcachedClient client = new XMemcachedClient(ip, port);
		for (int i = 0; i < num; i++)
			new Thread(new TestThread(i, client, barrier)).start();
		long start = System.currentTimeMillis();
		barrier.await();

		barrier.await();
		client.shutdown();
		System.out.println("timed:" + (System.currentTimeMillis() - start));
	}
}
