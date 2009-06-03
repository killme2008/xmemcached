package net.rubyeye.memcached.benchmark.java_memcached;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;

import net.rubyeye.memcached.benchmark.Constants;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import com.google.code.yanf4j.util.ResourcesUtils;

public class JavaMemCached implements Constants {

	public static void main(String[] args) throws Exception {
		Properties properties = ResourcesUtils
				.getResourceAsProperties("memcached.properties");
		if (properties.get("servers") == null) {
			System.err.print("Please config the memcached.properties");
			System.exit(1);
		}

		String servers = (String) properties.get("servers");
		BasicConfigurator.configure();
		SockIOPool pool = SockIOPool.getInstance();
		pool.setServers(servers.split(" "));
		MemCachedClient memcachedClient = new MemCachedClient();

		warmUp(memcachedClient);

		for (int i = 0; i < THREADS.length; i++) {
			for (int j = 0; j < BYTES.length; j++) {
				int t = (int) Math.log(THREADS[i]);
				int repeats = 100000 * (t <= 0 ? 1 : t);
				test(memcachedClient, BYTES[j], THREADS[i], repeats, true);
			}
		}
		pool.shutDown();
	}

	private static void warmUp(MemCachedClient memcachedClient)
			throws Exception {
		test(memcachedClient, 100, 100, 10000, false);
		System.out.println("warm up");
	}

	public static void test(MemCachedClient memcachedClient, int length,
			int threads, int repeats, boolean print) throws Exception {
		memcachedClient.flushAll();
		CountDownLatch latch = new CountDownLatch(threads);
		AtomicLong miss = new AtomicLong(0);
		long start = System.nanoTime();
		for (int i = 0; i < threads; i++) {
			new ReadWriteThread(memcachedClient, repeats, latch, i * repeats,
					length, miss).start();
		}
		latch.await();
		if (print) {
			long duration = System.nanoTime() - start;
			long total = repeats * threads;
			System.out.println("Spymemcached done,threads=" + threads
					+ ",repeats=" + repeats + ",valueLength=" + length
					+ ",tps=" + total * 1000000000 / duration + ",miss="
					+ miss.get());
		}
	}
}
