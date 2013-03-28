package net.rubyeye.memcached;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.memcached.benchmark.StringGenerator;

public abstract class BaseReadWriteThread extends Thread {

	protected int repeats;
	protected CyclicBarrier barrier;
	protected int offset;
	protected int length;
	protected AtomicLong miss;
	protected AtomicLong fail;
	protected AtomicLong hit;

	public BaseReadWriteThread(int repeats, CyclicBarrier barrier, int offset,
			int length, AtomicLong miss, AtomicLong fail, AtomicLong hit) {
		super();
		this.repeats = repeats;
		this.barrier = barrier;
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
			barrier.await();
			int writeMax = offset + writeTimes;
			for (int i = offset; i <= writeMax; i++) {
				String s = StringGenerator.generate(i, length);
				if (!set(i, s)) {
					System.err.println("set error");
					System.exit(1);
				}
			}
			int countMax = (int) ((1 - Constants.WRITE_RATE) / Constants.WRITE_RATE);
			for (int count = 0; count < countMax; count++) {
				for (int i = offset; i <= writeMax; i++) {
					String s = StringGenerator.generate(i, length);
					String result = get(i);
					if (result != null && !s.equals(result)) {
						System.err.println("get error,expected " + s
								+ ",actual " + result);
						fail.incrementAndGet();
					} else if (result == null) {
						miss.incrementAndGet();
					} else if (result != null && result.equals(s)) {
						hit.incrementAndGet();
					}
				}
			}

			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

}
