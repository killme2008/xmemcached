package net.rubyeye.memcached.benchmark.java_memcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import com.danga.MemCached.MemCachedClient;

import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.memcached.benchmark.StringGenerator;

public class ReadWriteThread extends Thread {
	MemCachedClient memcachedClient;

	int repeats;

	CountDownLatch latch;

	int offset;

	int length;
	
	AtomicLong miss ;

	public ReadWriteThread(MemCachedClient memcachedClient, int repeats,
			CountDownLatch latch, int offset, int length,AtomicLong miss) {
		super();
		this.memcachedClient = memcachedClient;
		this.repeats = repeats;
		this.latch = latch;
		this.offset = offset;
		this.length = length;
		this.miss=miss;
	}

	public void run() {
		int writeTimes = (int) (this.repeats * Constants.WRITE_RATE);
		try {
			for (int i = offset; i <= offset + writeTimes; i++) {
				String s = StringGenerator.generate(i, length);
				if (!memcachedClient.set(String.valueOf(i), s, 0)) {
					System.err.println("set error");
					System.exit(1);
				}
			}
			for (int i = 0; i < repeats - writeTimes; i++) {
				int n = (i + offset) % writeTimes + offset;
				String s = StringGenerator.generate(n, length);
				String result = (String) this.memcachedClient.get(String
						.valueOf(n));
				if (result != null && !s.equals(result)) {
					System.err.println("get error,expected " + s + ",actual "
							+ result);
					System.exit(1);
				}else
					this.miss.incrementAndGet();

			}
			latch.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
