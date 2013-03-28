package net.rubyeye.memcached.benchmark.spymemcached;

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.BaseTest;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import com.google.code.yanf4j.util.ResourcesUtils;

public class Spymemcached extends BaseTest {

	public static void main(String[] args) throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("memcached.properties");
		if (properties.get("servers") == null) {
			System.err.print("Please config the memcached.properties");
			System.exit(1);
		}
		String servers = (String) properties.get("servers");
		MemcachedClient memcachedClient = new MemcachedClient(AddrUtil
				.getAddresses(servers));
		System.out.println("Spymemcached startup");
		warmUp(memcachedClient);

		for (int i = 0; i < THREADS.length; i++) {
			for (int j = 0; j < BYTES.length; j++) {
				int repeats = getReapts(i);
				test(memcachedClient, BYTES[j], THREADS[i], repeats, true);
			}
		}
		memcachedClient.shutdown();

	}

	private static void warmUp(MemcachedClient memcachedClient)
			throws Exception {
		test(memcachedClient, 100, 100, 10000, false);
		System.out.println("warm up");
	}

	public static void test(MemcachedClient memcachedClient, int length,
			int threads, int repeats, boolean print) throws Exception {
		memcachedClient.flush();
		AtomicLong miss = new AtomicLong(0);
		AtomicLong fail = new AtomicLong(0);
		AtomicLong hit = new AtomicLong(0);
		CyclicBarrier barrier = new CyclicBarrier(threads + 1);

		for (int i = 0; i < threads; i++) {
			new ReadWriteThread(memcachedClient, repeats, barrier, i * repeats,
					length, miss, fail, hit).start();
		}
		barrier.await();
		long start = System.nanoTime();
		barrier.await();
		if (print) {
			long duration = System.nanoTime() - start;
			long total = repeats * threads;
			printResult(length, threads, repeats, miss, fail, hit, duration,
					total);
		}
	}
}
