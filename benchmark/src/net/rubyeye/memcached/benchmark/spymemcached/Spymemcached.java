package net.rubyeye.memcached.benchmark.spymemcached;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.benchmark.Constants;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;

import com.google.code.yanf4j.util.ResourcesUtils;

public class Spymemcached implements Constants {

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
		memcachedClient.flush();
		CountDownLatch latch = new CountDownLatch(threads);
		AtomicLong miss = new AtomicLong(0);
		AtomicLong fail = new AtomicLong(0);
		long start = System.nanoTime();
		for (int i = 0; i < threads; i++) {
			new ReadWriteThread(memcachedClient, repeats, latch, i * repeats,
					length, miss,fail).start();
		}
		latch.await();
		if (print) {
			long duration = System.nanoTime() - start;
			long total = repeats * threads;
			System.out.println("threads=" + threads
					+ ",repeats=" + repeats + ",valueLength=" + length
					+ ",tps=" + total * 1000000000 / duration + ",miss="
					+ miss.get()+",fail="+fail.get());
		}
	}
}
