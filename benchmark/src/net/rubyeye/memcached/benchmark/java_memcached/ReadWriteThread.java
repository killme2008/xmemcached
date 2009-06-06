package net.rubyeye.memcached.benchmark.java_memcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import com.danga.MemCached.MemCachedClient;

import net.rubyeye.memcached.BaseReadWriteThread;


public class ReadWriteThread extends BaseReadWriteThread {
	MemCachedClient memcachedClient;

	public ReadWriteThread(MemCachedClient memcachedClient, int repeats,
			CountDownLatch latch, int offset, int length, AtomicLong miss,
			AtomicLong fail, AtomicLong hit) {
		super(repeats, latch, offset, length, miss, fail, hit);
		this.memcachedClient = memcachedClient;
	}

	public String get(int n) throws Exception {
		String result = (String) this.memcachedClient.get(String.valueOf(n));
		return result;
	}

	public boolean set(int i, String s) throws Exception {
		return memcachedClient.set(String.valueOf(i), s);
	}
}
