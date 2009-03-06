package net.rubyeye.xmemcached.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.XMemcachedClient;

public class PerformanceTest2 {
	static Map<String, NameClass> map2 = new HashMap<String, NameClass>();
	static final int ELEMENT_NUM = 100;
	static {
		for (int i = 0; i < ELEMENT_NUM; i++)
			map2.put(String.valueOf(i), new NameClass(String.valueOf(i), String
					.valueOf(i)));
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
					String key = String.valueOf(start + i);
					if (!mc.set(key, 0, map2)) {
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

		@SuppressWarnings("unchecked")
		public void run() {
			try {
				for (int i = 0; i < repeat; i++) {

					String key = String.valueOf(start + i);
					Map<String, NameClass> result = (Map<String, NameClass>) mc
							.get(key);
					if (result.size() != ELEMENT_NUM) {
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

	static class TestDeleteRunnable implements Runnable {

		private XMemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		public TestDeleteRunnable(XMemcachedClient mc, int start,
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
					String key = String.valueOf(start + i);
					if (!mc.delete(key))
						System.err.println("delete error");
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

			int thread = 100;

			int repeat = 100;

			XMemcachedClient mc = new XMemcachedClient(ip, port);

			CountDownLatch cdl = new CountDownLatch(thread);
			long t = System.currentTimeMillis();
			for (int i = 0; i < thread; i++) {
				new Thread(new PerformanceTest2.TestWriteRunnable(mc,
						i * 10000, cdl, repeat)).start();
			}
			try {
				cdl.await();
			} catch (InterruptedException e) {

			}
			long all = thread * repeat;
			long usingtime = (System.currentTimeMillis() - t);

			System.out
					.println(String
							.format(
									"test write,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
									thread, repeat, size, all, 1000 * all
											/ usingtime, usingtime));

			// ���Զ�
			cdl = new CountDownLatch(thread);
			t = System.currentTimeMillis();
			for (int i = 0; i < thread; i++) {
				new Thread(new PerformanceTest2.TestReadRunnable(mc, i * 10000,
						cdl, repeat)).start();
			}
			try {
				cdl.await();
			} catch (InterruptedException e) {

			}
			all = thread * repeat;
			usingtime = (System.currentTimeMillis() - t);
			System.out
					.println(String
							.format(
									"test read,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
									thread, repeat, size, all, 1000 * all
											/ usingtime, usingtime));
			// // ����ɾ��
			// cdl = new CountDownLatch(thread);
			// t = System.currentTimeMillis();
			// for (int i = 0; i < thread; i++) {
			// new Thread(new PerformanceTest2.TestDeleteRunnable(mc,
			// i * 10000, cdl, repeat)).start();
			// }
			// try {
			// cdl.await();
			// } catch (InterruptedException e) {
			//
			// }
			// all = thread * repeat;
			// usingtime = (System.currentTimeMillis() - t);
			// System.out
			// .println(String
			// .format(
			// "test delete,thread num=%d, repeat=%d,size=%d, all=%d
			// ,velocity=%d , using time:%d",
			// thread, repeat, size, all, 1000 * all
			//											/ usingtime, usingtime));

			mc.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
