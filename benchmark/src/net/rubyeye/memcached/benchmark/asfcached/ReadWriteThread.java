package net.rubyeye.memcached.benchmark.asfcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import com.alisoft.xplatform.asf.cache.memcached.client.MemCachedClient;

import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.memcached.benchmark.StringGenerator;

public class ReadWriteThread extends Thread {
	MemCachedClient memcachedClient;

	int repeats;

	CountDownLatch latch;

	int offset;

	int length;

	AtomicLong miss;
	
	AtomicLong fail;

	public ReadWriteThread(MemCachedClient memcachedClient, int repeats,
			CountDownLatch latch, int offset, int length, AtomicLong miss,AtomicLong fail) {
		super();
		this.memcachedClient = memcachedClient;
		this.repeats = repeats;
		this.latch = latch;
		this.offset = offset;
		this.length = length;
		this.fail=fail;
		this.miss = miss;
	}

	public void run() {
		int writeTimes = (int) (this.repeats * Constants.WRITE_RATE);
		try {
			int writeMax = offset + writeTimes;
			int readTimes = this.repeats - writeTimes;
			for (int i = offset; i <= writeMax; i++) {
				String s = StringGenerator.generate(i, length);
				if (!memcachedClient.set(String.valueOf(i), s)) {
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
				String result = (String)this.memcachedClient.get(String.valueOf(i));
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
