package net.rubyeye.memcached.benchmark.xmemcached;

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.BaseTest;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

import com.google.code.yanf4j.util.ResourcesUtils;

public class Xmemcached extends BaseTest {
	public static void main(String[] args) throws Exception {
		boolean useBinaryProtocl = false;
		int connCount = 1;
		if (args.length >= 1) {
			if (args[0].equals("binary")) {
				useBinaryProtocl = true;
			}
		}
		if (args.length >= 2) {
			connCount = Integer.parseInt(args[1]);
		}
		Properties properties = ResourcesUtils
				.getResourceAsProperties("memcached.properties");
		if (properties.get("servers") == null) {
			System.err.print("Please config the memcached.properties");
			System.exit(1);
		}
		String servers = (String) properties.get("servers");
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(servers));
		builder.setConnectionPoolSize(connCount);
		if (useBinaryProtocl) {
			builder.setCommandFactory(new BinaryCommandFactory());
		}

		MemcachedClient memcachedClient = builder.build();
		System.out.println("Xmemcached startup");
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
