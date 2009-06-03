package net.rubyeye.memcached.benchmark.xmemcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.memcached.benchmark.StringGenerator;
import net.rubyeye.xmemcached.MemcachedClient;

public class ReadWriteThread extends Thread {
	MemcachedClient memcachedClient;

	int repeats;

	CountDownLatch latch;

	int offset;

	int length;

	AtomicLong miss;

	public ReadWriteThread(MemcachedClient memcachedClient, int repeats,
			CountDownLatch latch, int offset, int length, AtomicLong miss) {
		super();
		this.memcachedClient = memcachedClient;
		this.repeats = repeats;
		this.latch = latch;
		this.offset = offset;
		this.length = length;
		this.miss = miss;
	}

	public void run() {
		int writeTimes = (int) (this.repeats * Constants.WRITE_RATE);
		try {
			for (int i = offset; i <= offset + writeTimes; i++) {
				String s = StringGenerator.generate(i, length);
				if (!memcachedClient.set(String.valueOf(i), 0, s)) {
					System.err.println("set error");
					System.exit(1);
				}
			}
			for (int i = 0; i < repeats - writeTimes; i++) {
				int n = (i + offset) % writeTimes + offset;
				String s = StringGenerator.generate(n, length);
				String result = this.memcachedClient.get(String.valueOf(n));
				if (result != null && !s.equals(result)) {
					System.err.println("get error,expected " + s + ",actual "
							+ result);
					System.exit(1);
				} else {
					miss.incrementAndGet();
				}

			}
			latch.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
