package net.rubyeye.memcached.benchmark.java_memcached;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import net.rubyeye.memcached.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaMemCached extends BaseTest {

	private static Logger logger = LoggerFactory.getLogger("main");

	public static void main(String[] args) throws Exception {
		int maxConn=500;

		String servers = (args.length > 0 && args[0] != null) ? args[0] : "localhost:11211";
		SockIOPool pool = SockIOPool.getInstance();
		pool.setMinConn(10);
		pool.setMaxConn(maxConn);
		pool.setMaxIdle(60 * 60 * 1000);
		pool.setServers(servers.split(" "));
		pool.initialize();

		MemCachedClient memcachedClient = new MemCachedClient();
		memcachedClient.setCompressThreshold(16 * 1024);
		logger.info("Java-MemCached startup");
		warmUp(memcachedClient);
		printHeader();

		for (int i = 0; i < THREADS.length; i++) {
			for (int j = 0; j < BYTES.length; j++) {
				int repeats = getReapts(i);
				test(memcachedClient, BYTES[j], THREADS[i], repeats, true);
			}
		}
		pool.shutDown();
	}

	private static void warmUp(MemCachedClient memcachedClient)
			throws Exception {
		test(memcachedClient, 100, 100, 10000, false);
		logger.info("warm up");
	}

	public static void test(MemCachedClient memcachedClient, int length,
			int threads, int repeats, boolean print) throws Exception {
		memcachedClient.flushAll();
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
