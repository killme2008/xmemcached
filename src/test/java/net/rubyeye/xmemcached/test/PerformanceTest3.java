package net.rubyeye.xmemcached.test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * test multi get
 *
 * @author dennis
 *
 */
public class PerformanceTest3 {

	public static void main(String[] args) throws Exception {

		if (args.length < 4) {
			System.err
					.println("Useage:java PerformanceTest3 [server] [repeats] [threads] [count]");
			System.exit(1);
		}
		String server = args[0];
		int repeat = Integer.parseInt(args[1]);
		int threads = Integer.parseInt(args[2]);
		int count = Integer.parseInt(args[3]);
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(server));
		MemcachedClient client = builder.build();
		List<String> keys = new ArrayList<String>();

		for (int i = 0; i < count; i++) {
			client.set(String.valueOf(i), 0, new NameClass(String.valueOf(i),
					String.valueOf(i)));
			keys.add(String.valueOf(i));
		}
		CyclicBarrier barrier = new CyclicBarrier(threads + 1);

		for (int i = 0; i < threads; i++) {
			new ReadTest(barrier, client, count, keys, repeat).start();
		}
		long start = System.currentTimeMillis();
		barrier.await();
		barrier.await();
		long time = System.currentTimeMillis() - start;
		long all = threads * repeat;
		System.out.println("velocity=" + all*1000 / time );
		client.shutdown();
	}

	static class ReadTest extends Thread {
		private MemcachedClient client;
		int repeat;
		List<String> keys;
		int count;
		CyclicBarrier barrier;

		public ReadTest(CyclicBarrier barrier, MemcachedClient client,
				int count, List<String> keys, int repeat) {
			super();
			this.barrier = barrier;
			this.client = client;
			this.count = count;
			this.keys = keys;
			this.repeat = repeat;
		}

		public void run() {
			try {
				barrier.await();
				Map<String, NameClass> result = multiGet(repeat, count, client,
						keys);
				if (result.size() != count)
					System.err.println("multi get error");
				barrier.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<String, NameClass> multiGet(int repeat, int count,
			MemcachedClient client, List<String> keys) throws TimeoutException,
			InterruptedException, MemcachedException {
		Map<String, NameClass> result = null;
		for (int i = 0; i < repeat; i++) {
			result = client.get(keys, 10000);
			if (result.size() != count)
				System.err.println("multi get error");
		}
		return result;
	}
}
