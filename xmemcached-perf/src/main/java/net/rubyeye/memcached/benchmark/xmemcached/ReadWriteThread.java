package net.rubyeye.memcached.benchmark.xmemcached;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.BaseReadWriteThread;
import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.xmemcached.MemcachedClient;

public class ReadWriteThread extends BaseReadWriteThread {
	MemcachedClient memcachedClient;

	public ReadWriteThread(MemcachedClient memcachedClient, int repeats,
			CyclicBarrier barrier, int offset, int length, AtomicLong miss,
			AtomicLong fail, AtomicLong hit) {
		super(repeats, barrier, offset, length, miss, fail, hit);
		this.memcachedClient = memcachedClient;
	}

	public boolean set(int i, String s) throws Exception {
		return memcachedClient.set(String.valueOf(i), 0, s,
				Constants.OP_TIMEOUT);
	}

	public String get(int n) throws Exception {
		String result = this.memcachedClient.get(String.valueOf(n),
				Constants.OP_TIMEOUT);
		return result;
	}
}
