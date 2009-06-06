package net.rubyeye.memcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.memcached.benchmark.StringGenerator;

public abstract class BaseReadWriteThread extends Thread {

	protected int repeats;
	protected CountDownLatch latch;
	protected int offset;
	protected int length;
	protected AtomicLong miss;
	protected AtomicLong fail;
	protected AtomicLong hit;

	public BaseReadWriteThread(int repeats, CountDownLatch latch, int offset,
			int length, AtomicLong miss, AtomicLong fail, AtomicLong hit) {
		super();
		this.repeats = repeats;
		this.latch = latch;
		this.offset = offset;
		this.length = length;
		this.miss = miss;
		this.fail = fail;
		this.hit = hit;
	}

	public abstract boolean set(int i, String s) throws Exception;

	public abstract String get(int n) throws Exception;

	public void run() {
		int writeTimes = (int) (this.repeats * Constants.WRITE_RATE);
		try {
			int writeMax = offset + writeTimes;
			int readTimes = this.repeats - writeTimes;
			for (int i = offset; i <= writeMax; i++) {
				String s = StringGenerator.generate(i, length);
				if (!set(i, s)) {
					System.err.println("set error");
					System.exit(1);
				}
			}
			for (int i = 0; i <= readTimes; i++) {
				int n = (i+offset) > writeMax ? ((i+offset) % writeMax) : (i+offset);
				String s = StringGenerator.generate(n, length);
				String result = get(n);
				if (result != null && !s.equals(result)) {
					System.err.println("get error,expected " + s + ",actual "
							+ result);
					fail.incrementAndGet();
				} else if (result == null) {
					miss.incrementAndGet();
				} else if (result != null && result.equals(s)) {
					hit.incrementAndGet();
				} 
			}
			latch.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
