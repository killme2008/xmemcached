package net.rubyeye.memcached.benchmark.spymemcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.BaseReadWriteThread;
import net.rubyeye.memcached.benchmark.Constants;
import net.spy.memcached.MemcachedClient;

public class ReadWriteThread extends BaseReadWriteThread {
	MemcachedClient memcachedClient;

	public ReadWriteThread(MemcachedClient memcachedClient, int repeats,
			CountDownLatch latch, int offset, int length, AtomicLong miss,
			AtomicLong fail, AtomicLong hit) {
		super(repeats, latch, offset, length, miss, fail, hit);
		this.memcachedClient = memcachedClient;

	}

	public String get(int n) throws Exception {
		String result = (String) this.memcachedClient.asyncGet(
				String.valueOf(n)).get(Constants.OP_TIMEOUT,
				TimeUnit.MILLISECONDS);
		return result;
	}

	public boolean set(int i, String s) throws Exception {
		return memcachedClient.set(String.valueOf(i), 0, s).get(
				Constants.OP_TIMEOUT, TimeUnit.MILLISECONDS);
	}
}
