package net.rubyeye.xmemcached.test.legacy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class PerformanceTest2 {
	static Map<String, NameClass> map2 = new HashMap<String, NameClass>();
	static final int ELEMENT_NUM = 50;
	static {
		for (int i = 0; i < ELEMENT_NUM; i++) {
			map2.put(String.valueOf(i), new NameClass(String.valueOf(i), String
					.valueOf(i)));
		}
	}

	static class TestWriteRunnable implements Runnable {

		private MemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		public TestWriteRunnable(MemcachedClient mc, int start,
				CountDownLatch cdl, int repeat) {
			super();
			this.mc = mc;
			this.start = start;
			this.cd = cdl;
			this.repeat = repeat;

		}

		public void run() {
			try {

				for (int i = 0; i < this.repeat; i++) {
					String key = String.valueOf(this.start + i);
					if (!this.mc.set(key, 0, map2, 5000)) {
						System.err.println("set error");
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.cd.countDown();
			}
		}

	}

	static class TestReadRunnable implements Runnable {

		private MemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		public TestReadRunnable(MemcachedClient mc, int start,
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
				for (int i = 0; i < this.repeat; i++) {

					String key = String.valueOf(this.start + i);
					Map<String, NameClass> result = (Map<String, NameClass>) this.mc
							.get(key, 5000);
					if (result == null || result.size() != ELEMENT_NUM) {
						System.err.println("get " + key + " error");
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.cd.countDown();
			}
		}

	}

	// thread num=10, repeat=10000,size=2, all=200000 ,velocity=1057 , using
	// time:189187
	static public void main(String[] args) {
		try {

			if (args.length < 3) {
				System.err
						.println("Useage:java Performance2 [threads] [repeats][servers]");
				System.exit(1);
			}
			int cpuCount = Runtime.getRuntime().availableProcessors();

			int thread = Integer.parseInt(args[0]);

			int repeat = Integer.parseInt(args[1]);

			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(args[2]));
			builder.setCommandFactory(new BinaryCommandFactory());
			// builder.getConfiguration().setReadThreadCount(0);
			MemcachedClient mc = builder.build();
			//mc.setOptimizeGet(false);
			testWrite(cpuCount, thread, repeat, mc);
			testRead(cpuCount, thread, repeat, mc);
			// mc.flushAll(10000); // delete all
			// System.out.println(mc.stats("192.168.207.101:12000"));
			mc.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testRead(int size, int thread, int repeat,
			MemcachedClient mc) {
		CountDownLatch cdl;
		long t;
		long all;
		long usingtime;
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
	}

	private static void testWrite(int size, int thread, int repeat,
			MemcachedClient mc) {
		CountDownLatch cdl = new CountDownLatch(thread);
		long t = System.currentTimeMillis();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest2.TestWriteRunnable(mc, i * 10000,
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
	}
}
