package net.rubyeye.memcached.benchmark.xmemcached;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import com.google.code.yanf4j.util.ResourcesUtils;

import net.rubyeye.memcached.BaseTest;
import net.rubyeye.memcached.benchmark.Constants;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class Xmemcached extends BaseTest implements Constants {
	public static void main(String[] args) throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("memcached.properties");
		if (properties.get("servers") == null) {
			System.err.print("Please config the memcached.properties");
			System.exit(1);
		}
		String servers = (String) properties.get("servers");
		MemcachedClient memcachedClient = new XMemcachedClient(AddrUtil
				.getAddresses(servers));
		System.out.println("Xmemcached startup");
		warmUp(memcachedClient);

		for (int i = 0; i < THREADS.length; i++) {
			for (int j = 0; j < BYTES.length; j++) {
				int t = (int) Math.log(THREADS[i]);
				int repeats = BASE_REPEATS * (t <= 0 ? 1 : t);
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
		memcachedClient.flushAll();
		AtomicLong miss = new AtomicLong(0);
		AtomicLong fail = new AtomicLong(0);
		AtomicLong hit = new AtomicLong(0);
		CountDownLatch countDownLatch = new CountDownLatch(threads);
		long start = System.nanoTime();
		for (int i = 0; i < threads; i++) {
			new ReadWriteThread(memcachedClient, repeats, countDownLatch, i
					* repeats, length, miss, fail, hit).start();
		}
		countDownLatch.await();
		if (print) {
			long duration = System.nanoTime() - start;
			long total = repeats * threads;
			printResult(length, threads, repeats, miss, fail, hit, duration,
					total);
		}
	}
}
