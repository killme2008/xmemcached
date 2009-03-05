package net.rubyeye.xmemcached.test;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.XMemcachedClient;

public class PerformanceTest {

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
					if (!mc.set(key, 0, key)) {
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

					String key = String.valueOf(start + i);
					String result = (String) mc.get(key,100000000);
					if (!key.equals(result)) {
						System.out.println(key + " " + result);
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
			String ip = "localhost";

			int port = 11211;

			int size = Runtime.getRuntime().availableProcessors();

			int thread = 50;

			int repeat = 10000;

			XMemcachedClient mc = new XMemcachedClient(ip, port);

			CountDownLatch cdl = new CountDownLatch(thread);
			long t = System.currentTimeMillis();
			for (int i = 0; i < thread; i++) {
				new Thread(new PerformanceTest.TestWriteRunnable(mc, i * 10000,
						cdl, repeat)).start();
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

			cdl = new CountDownLatch(thread);
			t = System.currentTimeMillis();
			for (int i = 0; i < thread; i++) {
				new Thread(new PerformanceTest.TestReadRunnable(mc, i * 10000,
						cdl, repeat)).start();
			}
			try {
				cdl.await();
			} catch (InterruptedException e) {

			}
                    all = thread * repeat;
                    usingtime = (System.currentTimeMillis() - t);
                    System.out.println(String.format(
                            "test read,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
                            thread, repeat, size, all, 1000 * all / usingtime, usingtime));
                    cdl = new CountDownLatch(thread);
                    t = System.currentTimeMillis();
                    for (int i = 0; i < thread; i++) {
                        new Thread(new PerformanceTest.TestDeleteRunnable(mc,
                                i * 10000, cdl, repeat)).start();
                    }
                    try {
                        cdl.await();
                    } catch (InterruptedException e) {
                    }
                    all = thread * repeat;
                    usingtime = (System.currentTimeMillis() - t);
                    System.out.println(String.format(
                            "test delete,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
                            thread, repeat, size, all, 1000 * all / usingtime, usingtime));

			mc.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
