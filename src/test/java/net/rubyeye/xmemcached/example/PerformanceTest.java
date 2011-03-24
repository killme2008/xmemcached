package net.rubyeye.xmemcached.example;

import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class PerformanceTest {
	public static void main(String[] args) throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses("localhost:12000 localhost:12001"));
		MemcachedClient client = builder.build();
		final AtomicInteger failCount = new AtomicInteger();
		Thread thread = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {

					}
					System.out.println(failCount.get());
				}

			}
		};
		thread.start();
		for (int i = 0;; i++) {
			try {
				client.set(String.valueOf(i), 0, i);
				client.delete(String.valueOf(i));
			} catch (Exception e) {
				failCount.incrementAndGet();
			}
		}

	}
}
