package net.rubyeye.xmemcached.test.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

import com.google.code.yanf4j.util.ResourcesUtils;

class TestThread implements Runnable {
	private static final int ELEMENT_NUM = 10;
	MemcachedClient xmemcachedClient;
	CyclicBarrier barrier;

	static List<Integer> list = new ArrayList<Integer>();
	static Map<String, Integer> map = new HashMap<String, Integer>();
	static Map<String, NameClass> map2 = new HashMap<String, NameClass>();

	static {
		for (int i = 0; i < ELEMENT_NUM; i++) {
			list.add(i);
		}
		for (int i = 0; i < ELEMENT_NUM; i++) {
			map.put(String.valueOf(i), i);
		}
		for (int i = 0; i < ELEMENT_NUM; i++) {
			map2.put(String.valueOf(i), new NameClass(String.valueOf(i), String
					.valueOf(i)));
		}
	}

	int number;

	final static int NUM = 1000;

	public TestThread(int number, MemcachedClient xmemcachedClient,
			CyclicBarrier barrier) {
		this.xmemcachedClient = xmemcachedClient;
		this.number = number;
		this.barrier = barrier;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			this.barrier.await();
			for (int i = 0; i < NUM; i++) {
				assert (this.xmemcachedClient.set("test_" + this.number + "_" + i,
						0, i, 5000));
			}
			for (int i = 0; i < NUM; i++) {
				assert ((Integer) this.xmemcachedClient.get("test_" + this.number
						+ "_" + i, 2000) == i);
			}
			for (int i = 0; i < NUM; i++) {
				assert (this.xmemcachedClient
						.delete("test_" + this.number + "_" + i));
			}
			for (int i = 0; i < NUM; i++) {
				assert ((Integer) this.xmemcachedClient.get("test_" + this.number
						+ "_" + i, 2000) == null);
			}
			assert (this.xmemcachedClient.set("list_" + this.number, 0, list));
			List<Integer> cachedList = (List<Integer>) this.xmemcachedClient.get(
					"list_" + this.number, 2000);
			assert (cachedList.size() == ELEMENT_NUM);
			for (int i = 0; i < ELEMENT_NUM; i++) {
				assert (cachedList.get(i) == i);
			}
			assert (this.xmemcachedClient.delete("list_" + this.number));

			assert (this.xmemcachedClient.set("map_" + this.number, 0, map));
			Map<String, Integer> cachedMap = (Map<String, Integer>) this.xmemcachedClient
					.get("map_" + this.number, 2000);
			assert (cachedMap.size() == ELEMENT_NUM);
			for (int i = 0; i < ELEMENT_NUM; i++) {
				assert (cachedMap.get(String.valueOf(i)) == i);
			}
			assert (this.xmemcachedClient.delete("map_" + this.number));

			assert (this.xmemcachedClient.set("map2_" + this.number, 0, map2));
			Map<String, NameClass> cachedMap2 = (Map<String, NameClass>) this.xmemcachedClient
					.get("map2_" + this.number, 2000);
			assert (cachedMap2.size() == ELEMENT_NUM);
			for (int i = 0; i < ELEMENT_NUM; i++) {
				assert ((cachedMap2.get(String.valueOf(i))).firstName
						.equals(String.valueOf(i)));
			}
			assert (this.xmemcachedClient.delete("map2_" + this.number));
			this.barrier.await();
		} catch (Exception e) {
			if (!(e instanceof TimeoutException)) {
				e.printStackTrace();
			// System.err.println(this.number + " " + e.getMessage());
			}
		}
	}
}

public class XMemcachedClientThreadSafeTest {
	public static void main(String args[]) throws Exception {
		String servers = ResourcesUtils.getResourceAsProperties(
				"test.properties").getProperty("test.memcached.servers");
		int num = 1000;
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(servers));
		builder.setCommandFactory(new BinaryCommandFactory());
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		MemcachedClient client = builder.build();
		client.setOptimizeGet(false);

		for (int i = 0; i < num; i++) {
			new Thread(new TestThread(i, client, barrier)).start();
		}
		long start = System.currentTimeMillis();
		barrier.await();

		barrier.await();
		client.shutdown();
		System.out.println("timed:" + (System.currentTimeMillis() - start));
	}
}
