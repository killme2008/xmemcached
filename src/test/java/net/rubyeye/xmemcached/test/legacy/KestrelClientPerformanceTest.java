package net.rubyeye.xmemcached.test.legacy;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.KestrelCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class KestrelClientPerformanceTest {
	static int[] CLIENT_NUM = { 1, 10, 100 };

	static final int MESSAGE_COUNT = 500000;

	public static void main(String args[]) throws Exception {
		if (args.length < 2) {
			System.out
					.println("Useage:java KestrelClientPerformanceTest [messageSize] [kestrelServer]");
			System.exit(1);
		}
		StringBuilder sb = new StringBuilder();
		int messageSize = Integer.parseInt(args[0]);
		for (int i = 0; i < messageSize; i++) {
			;
		}
		sb.append("a");
		String message = sb.toString();
		for (int i = 0; i < CLIENT_NUM.length; i++) {
			MemcachedClient client = createClient(args[1], CLIENT_NUM[i]);
			client.flushAll(60000);
			long start = System.currentTimeMillis();
			for (int j = 0; j < MESSAGE_COUNT; j++) {
				if (!client.set("queue1", 0, message)) {
					throw new RuntimeException("Push fail");
				}
			}
			System.out.println("Pushes---Client connection:" + CLIENT_NUM[i]
					+ ",Message count:" + MESSAGE_COUNT + ",Message size:"
					+ messageSize + ",Total time:"
					+ (System.currentTimeMillis() - start));

			start = System.currentTimeMillis();
			for (int j = 0; j < MESSAGE_COUNT; j++) {
				if (client.get("queue1") == null) {
					throw new RuntimeException("fetch null");
				}
			}
			System.out.println("Fetches---Client connection:" + CLIENT_NUM[i]
					+ ",Message count:" + MESSAGE_COUNT + ",Message size:"
					+ messageSize + ",Total time:"
					+ (System.currentTimeMillis() - start));

			client.shutdown();

		}
	}

	public static MemcachedClient createClient(String server, int poolSize)
			throws Exception {
		MemcachedClientBuilder builder = newBuilder(server);
		builder.setConnectionPoolSize(poolSize);
		return builder.build();
	}

	private static MemcachedClientBuilder newBuilder(String server) {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(server));
		// Use kestrel command factory
		builder.setCommandFactory(new KestrelCommandFactory());
		return builder;
	}
}
