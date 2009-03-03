package net.rubyeye.xmemcached.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.XMemcachedClient;

public class PerformanceTest {

	static HashMap map = new HashMap();
	static {
		for (int i = 0; i < 100; i++)
			map.put(i, i);
	}

	static class TestWriteRunnable implements Runnable {

		private XMemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		public TestWriteRunnable(XMemcachedClient mc, int start,
				CountDownLatch cdl, int repeat) {
			super();
			this.mc = mc;
			this.start = start;
			this.cd = cdl;
			this.repeat = repeat;

		}

		public void run() {
			try {

				for (int i = 0; i < repeat; i++) {
					String key = String.valueOf(i);
					if (!mc.set(key, 0, map)) {
						System.err.println("set error");
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				cd.countDown();
			}
		}

	}

	static class TestReadRunnable implements Runnable {

		private XMemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		public TestReadRunnable(XMemcachedClient mc, int start,
				CountDownLatch cdl, int repeat) {
			super();
			this.mc = mc;
			this.start = start;
			this.cd = cdl;
			this.repeat = repeat;

		}

		public void run() {
			try {
				for (int i = 0; i < repeat; i++) {

					String key = String.valueOf(i);
					HashMap result = (HashMap) mc.get(key);
					if (result.size() != 100) {
						System.err.println("get error");
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				cd.countDown();
			}
		}

	}

	// thread num=10, repeat=10000,size=2, all=200000 ,velocity=1057 , using
	// time:189187
	static public void main(String[] args) {
		try {
			String ip = "192.168.222.100";

			int port = 11211;

			int size = Runtime.getRuntime().availableProcessors();

			int thread = 1;

			int repeat = 400;

			XMemcachedClient mc = new XMemcachedClient(ip, port);

			CountDownLatch cdl = new CountDownLatch(thread);
			long t = System.currentTimeMillis();
			// for (int i = 0; i < thread; i++) {
			// new Thread(new PerformanceTest.TestWriteRunnable(mc, i * 10000,
			// cdl, repeat)).start();
			// }

			for (int i = 0; i < thread; i++) {
				new Thread(new PerformanceTest.TestReadRunnable(mc, i * 10000,
						cdl, repeat)).start();
			}

			try {
				cdl.await();
			} catch (InterruptedException e) {

			}

			long all = 2 * thread * repeat;
			long usingtime = (System.currentTimeMillis() - t);

			System.out
					.println(String
							.format(
									"thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
									thread, repeat, size, all, 1000 * all
											/ usingtime, usingtime));
			// 测试批量获取
			t = System.currentTimeMillis();
			List<String> keys = new ArrayList<String>();
			keys.add("test");
			for (int i = 0; i < 400; i++)
				keys.add(String.valueOf(i));
			Map<String, Object> map = (HashMap<String, Object>) mc.get(keys);
			System.out.println("bulk get " + map.size() + " map:"
					+ (System.currentTimeMillis() - t));
			for (String key : map.keySet()) {
				assert (((Map) map.get(key)).size() == 100);
			}
			mc.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
