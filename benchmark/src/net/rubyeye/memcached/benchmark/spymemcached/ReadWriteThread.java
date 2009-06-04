package net.rubyeye.memcached.benchmark.spymemcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.memcached.benchmark.StringGenerator;
import net.spy.memcached.MemcachedClient;

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
			int writeMax = offset + writeTimes;
			for (int i = offset; i <= writeMax; i++) {
				String s = StringGenerator.generate(i, length);
				if (!memcachedClient.set(String.valueOf(i), 0, s).get(Constants.OP_TIMEOUT,TimeUnit.MILLISECONDS)) {
					System.err.println("set error");
					System.exit(1);
				}
			}
			for (int i = 0; i < repeats - writeTimes; i++) {
				int newOffset = i + offset;
				int n = (newOffset > writeMax) ? (newOffset % writeTimes + offset)
						: newOffset;
				String s = StringGenerator.generate(n, length);
				String result = (String) this.memcachedClient.asyncGet(String
						.valueOf(n)).get(Constants.OP_TIMEOUT,TimeUnit.MILLISECONDS);
				if (result != null && !s.equals(result)) {
					System.err.println("get error,expected " + s + ",actual "
							+ result);
					System.exit(1);
				} else
					miss.incrementAndGet();

			}
			latch.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
