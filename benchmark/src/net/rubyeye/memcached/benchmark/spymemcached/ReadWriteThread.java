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

	AtomicLong fail;

	public ReadWriteThread(MemcachedClient memcachedClient, int repeats,
			CountDownLatch latch, int offset, int length, AtomicLong miss,
			AtomicLong fail) {
		super();
		this.memcachedClient = memcachedClient;
		this.repeats = repeats;
		this.latch = latch;
		this.offset = offset;
		this.length = length;
		this.miss = miss;
		this.fail = fail;

	}

	public void run() {
		int writeTimes = (int) (this.repeats * Constants.WRITE_RATE);
		try {
			int writeMax = offset + writeTimes;
			int readTimes = this.repeats - writeTimes;
			for (int i = offset; i <= writeMax; i++) {
				String s = StringGenerator.generate(i, length);
				if (!memcachedClient.set(String.valueOf(i), 0, s).get(
						Constants.OP_TIMEOUT, TimeUnit.MILLISECONDS)) {
					System.err.println("set error");
					System.exit(1);
				}
			}
			int total = 0;
			for (int i = offset; i <= writeMax; i++) {
				total++;
				if (total > readTimes)
					break;
				String s = StringGenerator.generate(i, length);
				String result = (String) this.memcachedClient.asyncGet(
						String.valueOf(i)).get(Constants.OP_TIMEOUT,
						TimeUnit.MILLISECONDS);
				if (result != null && !s.equals(result)) {
					System.err.println("get error,expected " + s + ",actual "
							+ result);
					fail.incrementAndGet();
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
