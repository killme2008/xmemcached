package net.rubyeye.xmemcached.pressure;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class MemcachedClientPressureTest {
	private static final class ClockWatch implements Runnable {
		private long startTime;
		private long stopTime;

		public synchronized void run() {
			if (this.startTime == -1) {
				this.startTime = System.nanoTime();
			} else {
				this.stopTime = System.nanoTime();
			}

		}

		public synchronized void start() {
			this.startTime = -1;
		}

		public synchronized long getDurationInNano() {
			return this.stopTime - this.startTime;
		}

		public synchronized long getDurationInMillis() {
			return (this.stopTime - this.startTime) / 1000000;
		}
	}

	static class TestThread extends Thread {
		int repeat;
		int start;
		int size;
		MemcachedClient client;
		AtomicInteger failure;
		AtomicInteger success;
		CyclicBarrier barrier;

		public TestThread(int repeat, int start, int size,
				MemcachedClient client, AtomicInteger failure,
				AtomicInteger success, CyclicBarrier barrier) {
			super();
			this.repeat = repeat;
			this.start = start;
			this.size = size;
			this.client = client;
			this.failure = failure;
			this.success = success;
			this.barrier = barrier;
		}

		public void run() {
			byte[] value = new byte[size];
			try {
				barrier.await();
				for (int i = 0; i < repeat; i++) {
					String key = String.valueOf(start + i);
					try {
						if (!client.set(key, 10, value))
							throw new RuntimeException("set failed");
						byte[] v = client.get(key);
						if (v == null || v.length != size)
							throw new RuntimeException("get failed");
						if (!client.touch(key, 10))
							throw new RuntimeException("touch failed");
						if (!client.delete(key, 10))
							throw new RuntimeException("delete failed");
						success.incrementAndGet();
					} catch (Exception e) {
						e.printStackTrace();
						failure.incrementAndGet();
					}
				}
				barrier.await();
			} catch (Exception e) {
				// ignore;
			}
		}

	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw new RuntimeException("Please provide memcached servers.");
		}
		final int threads = 100;
		final int repeat = 50000;
		int size = 1024;

		String servers = args[0];
		final AtomicInteger failure = new AtomicInteger();
		final AtomicInteger success = new AtomicInteger();
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(
				AddrUtil.getAddresses(servers));
		//builder.setCommandFactory(new BinaryCommandFactory());
		MemcachedClient client = builder.build();
		ClockWatch watch = new ClockWatch();
		CyclicBarrier barrier = new CyclicBarrier(threads + 1, watch);
		for (int i = 0; i < threads; i++) {
			new TestThread(repeat, i * repeat * 2, size, client, failure,
					success, barrier).start();
		}
		new Thread() {
			public void run() {
				try {
					while (success.get() + failure.get() < repeat * threads) {
						Thread.sleep(1000);
						System.out.println("success:" + success.get()
								+ ",failure:" + failure.get());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		watch.start();
		barrier.await();
		barrier.await();

		long secs = watch.getDurationInMillis() / 1000;
		int total = 4 * repeat * threads;
		long tps = total / secs;
		client.shutdown();
		System.out.println("duration:" + secs + " seconds,tps:" + tps
				+ " op/seconds,total:" + total + " ops");

	}
}
