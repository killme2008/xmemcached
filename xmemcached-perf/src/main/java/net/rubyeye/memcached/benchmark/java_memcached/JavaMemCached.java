package net.rubyeye.memcached.benchmark.java_memcached;

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.BaseTest;
import net.rubyeye.memcached.benchmark.Constants;

import org.apache.log4j.BasicConfigurator;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import com.google.code.yanf4j.util.ResourcesUtils;

public class JavaMemCached extends BaseTest implements Constants {

	public static void main(String[] args) throws Exception {
		int maxConn=500;
		if(args.length>1){
			maxConn=Integer.parseInt(args[0]);
		}
		Properties properties = ResourcesUtils
				.getResourceAsProperties("memcached.properties");
		if (properties.get("servers") == null) {
			System.err.print("Please config the memcached.properties");
			System.exit(1);
		}

		String servers = (String) properties.get("servers");
		BasicConfigurator.configure();
		SockIOPool pool = SockIOPool.getInstance();
		pool.setMinConn(10);
		pool.setMaxConn(maxConn);
		pool.setMaxIdle(60 * 60 * 1000);
		pool.setServers(servers.split(" "));
		pool.initialize();

		MemCachedClient memcachedClient = new MemCachedClient();
		memcachedClient.setCompressThreshold(16 * 1024);
		System.out.println("Java-MemCached startup");
		warmUp(memcachedClient);

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
		System.out.println("warm up");
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
