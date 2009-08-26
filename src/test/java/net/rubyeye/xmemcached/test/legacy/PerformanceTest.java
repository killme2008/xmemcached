package net.rubyeye.xmemcached.test.legacy;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class PerformanceTest {

	/**
	 * 写线程
	 * 
	 * @author dennis
	 * 
	 */
	static class TestWriteRunnable implements Runnable {

		private MemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;
		int keySize;
		int valueSize;

		public TestWriteRunnable(MemcachedClient mc, int start,
				CountDownLatch cdl, int repeat, int keySize, int valueSize) {
			super();
			this.mc = mc;
			this.start = start;
			this.cd = cdl;
			this.repeat = repeat;
			this.keySize = keySize;
			this.valueSize = valueSize;

		}

		public void run() {
			try {

				for (int i = 0; i < this.repeat; i++) {
					 String key = getKey(this.start + i);
					 if (!this.mc.set(key, 0, getValue(this.start + i))) {
						System.err.println("set error");
						System.exit(1);
					}
			//		this.mc.setWithNoReply(key, 0, getValue(this.start + i));

				}

			} catch (Exception e) {
				 e.printStackTrace();
			} finally {
				this.cd.countDown();
			}
		}

		private final String getKey(int n) {
			StringBuilder sb = new StringBuilder(String.valueOf(n));
			while (sb.length() < this.keySize) {
				sb.append("k");
			}
			return sb.toString();
		}

		private final String getValue(int n) {
			StringBuilder sb = new StringBuilder(String.valueOf(n));
			while (sb.length() < this.valueSize) {
				sb.append("v");
			}
			return sb.toString();
		}
	}

	/**
	 * 读线程
	 * 
	 * @author dennis
	 * 
	 */
	static class TestReadRunnable implements Runnable {

		private MemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		int keySize;
		int valueSize;

		public TestReadRunnable(MemcachedClient mc, int start,
				CountDownLatch cdl, int repeat, int keySize, int valueSize) {
			super();
			this.mc = mc;
			this.start = start;
			this.cd = cdl;
			this.repeat = repeat;
			this.keySize = keySize;
			this.valueSize = valueSize;

		}

		private final String getKey(int n) {
			StringBuilder sb = new StringBuilder(String.valueOf(n));
			while (sb.length() < this.keySize) {
				sb.append("k");
			}
			return sb.toString();
		}

		private final String getValue(int n) {
			StringBuilder sb = new StringBuilder(String.valueOf(n));
			while (sb.length() < this.valueSize) {
				sb.append("v");
			}
			return sb.toString();
		}

		public void run() {
			try {
				for (int i = 0; i < this.repeat; i++) {

					String key = getKey(this.start+i);
					String result = (String) this.mc.get(key, 5000);
					if(result==null){
						System.out.println(key);
					}
					if (!result.equals(getValue(this.start+i))) {
						System.out.println(key + " " + result);
						System.err.println("get error");
						System.exit(1);
					}
				
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.cd.countDown();
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

		private MemcachedClient mc;
		private CountDownLatch cd;
		int repeat;
		int start;

		int keySize;
		int valueSize;

		public TestDeleteRunnable(MemcachedClient mc, int start,
				CountDownLatch cdl, int repeat, int keySize, int valueSize) {
			super();
			this.mc = mc;
			this.start = start;
			this.cd = cdl;
			this.repeat = repeat;
			this.keySize = keySize;
			this.valueSize = valueSize;

		}

		private final String getKey(int n) {
			StringBuilder sb = new StringBuilder(String.valueOf(n));
			while (sb.length() < this.keySize) {
				sb.append("k");
			}
			return sb.toString();
		}

		public void run() {
			try {
				for (int i = 0; i < this.repeat; i++) {
					String key = getKey(this.start + i);
					 if (!this.mc.delete(key)) {
						System.err.println("delete " + key + " error");
						System.exit(1);
					}
					//this.mc.deleteWithNoReply(key);
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.cd.countDown();
			}
		}
	}

	static public void main(String[] args) {
		if (args.length < 3) {
			System.out
					.println("Usage:[program name] threads totalTimes keySize valueSize servers");
			System.exit(1);
		}
		int thread = Integer.parseInt(args[0]); // 线程数
		int repeat = Integer.parseInt(args[1]) / thread; // 循环次数
		int keySize = Integer.parseInt(args[2]);
		int valueSize = Integer.parseInt(args[3]);
		try {
			int size = Runtime.getRuntime().availableProcessors();

			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(args[4]));
			builder.setCommandFactory(new BinaryCommandFactory());
			MemcachedClient mc = builder.build();
			mc.flushAll();
			//mc.setOptimizeGet(false);
			// mc.setOptimizeMergeBuffer(false);
			// 分别测试写、读、删除
			testWrite(thread, size, repeat, keySize, valueSize, mc);
			testRead(thread, size, repeat, keySize, valueSize, mc);
			testDelete(thread, size, repeat, keySize, valueSize, mc);

			mc.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testDelete(int thread, int size, int repeat,
			int keySize, int valueSize, MemcachedClient mc) {
		CountDownLatch cdl;
		long t;
		long all;
		long usingtime;
		cdl = new CountDownLatch(thread);
		t = System.nanoTime();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest.TestDeleteRunnable(mc, i * repeat,
					cdl, repeat, keySize, valueSize)).start();
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

	private static void testRead(int thread, int size, int repeat, int keySize,
			int valueSize, MemcachedClient mc) {
		CountDownLatch cdl = new CountDownLatch(thread);
		long t = System.nanoTime();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest.TestReadRunnable(mc, i * repeat,
					cdl, repeat, keySize, valueSize)).start();
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
			int keySize, int valueSize, MemcachedClient mc) {
		CountDownLatch cdl = new CountDownLatch(thread);
		long start = System.nanoTime();
		for (int i = 0; i < thread; i++) {
			new Thread(new PerformanceTest.TestWriteRunnable(mc, i * repeat,
					cdl, repeat, keySize, valueSize)).start();
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
