package net.rubyeye.xmemcached.test;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.buffer.CachedBufferAllocator;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class PerformanceTest {

	/**
	 * 写线程
	 * 
	 * @author dennis
	 * 
	 */
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

	/**
	 * 读线程
	 * 
	 * @author dennis
	 * 
	 */
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
					String result = (String) mc.get(key,5000);
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

	/**
	 * 删除线程
	 * 
	 * @author dennis
	 * 
	 */
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
					if (!mc.delete(key)) {
						System.err.println("delete error");
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				cd.countDown();
			}
		}
	}

	static public void main(String[] args) {
		if (args.length < 3) {
			System.out.println("Usage:[program name] threads repeats servers");
			System.exit(1);
		}
		int thread = Integer.parseInt(args[0]); // 线程数
		int repeat = Integer.parseInt(args[1]); // 循环次数
		try {
			int size = Runtime.getRuntime().availableProcessors();

			XMemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(args[2]));
			 builder.getConfiguration().setTcpRecvBufferSize(8*1024);
			builder.setBufferAllocator(new CachedBufferAllocator());
			builder.setSessionLocator(new KetamaMemcachedSessionLocator());
			XMemcachedClient mc = builder.build();
			// mc.setOptimizeMergeBuffer(false);
			// 分别测试写、读、删除
			testWrite(thread, size, repeat, mc);
			testRead(thread, size, repeat, mc);
			testDelete(thread, size, repeat, mc);

			mc.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testDelete(int thread, int size, int repeat,
			XMemcachedClient mc) {
		CountDownLatch cdl;
		long t;
		long all;
		long usingtime;
		cdl = new CountDownLatch(thread);
		t = System.nanoTime();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest.TestDeleteRunnable(mc, i * 10000,
					cdl, repeat)).start();
		}
		try {
			cdl.await();
		} catch (InterruptedException e) {
		}
		all = thread * repeat;
		usingtime = (System.nanoTime() - t);
		System.out
				.println(String
						.format(
								"test delete,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
								thread, repeat, size, all, 1000 * all
										/ (usingtime / 1000000), usingtime));
	}

	private static void testRead(int thread, int size, int repeat,
			XMemcachedClient mc) {
		CountDownLatch cdl = new CountDownLatch(thread);
		long t = System.nanoTime();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest.TestReadRunnable(mc, i * 10000, cdl,
					repeat)).start();
		}
		try {
			cdl.await();
		} catch (InterruptedException e) {
		}
		long all = thread * repeat;
		long usingtime = (System.nanoTime() - t);
		System.out
				.println(String
						.format(
								"test read,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
								thread, repeat, size, all, 1000 * all
										/ (usingtime / 1000000), usingtime));
	}

	private static void testWrite(int thread, int size, int repeat,
			XMemcachedClient mc) {
		CountDownLatch cdl = new CountDownLatch(thread);
		long start = System.nanoTime();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest.TestWriteRunnable(mc, i * 10000,
					cdl, repeat)).start();
		}
		try {
			cdl.await();
		} catch (InterruptedException e) {
		}
		long all = thread * repeat;
		long usingtime = (System.nanoTime() - start);

		System.out
				.println(String
						.format(
								"test write,thread num=%d, repeat=%d,size=%d, all=%d ,velocity=%d , using time:%d",
								thread, repeat, size, all, 1000 * all
										/ (usingtime / 1000000), usingtime));
	}
}
