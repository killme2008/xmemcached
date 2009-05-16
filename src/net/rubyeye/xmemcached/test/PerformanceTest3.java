package net.rubyeye.xmemcached.test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * test multi get
 *
 * @author dennis
 *
 */
public class PerformanceTest3 {

	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err
					.println("Useage:java PerformanceTest3 [server] [repeats]");
			System.exit(1);
		}
		String server = args[0];
		int repeat = Integer.parseInt(args[1]);
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(server));
		MemcachedClient client = builder.build();
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 500; i++) {
			client.set(String.valueOf(i), 0, new NameClass(String.valueOf(i),
					String.valueOf(i)));
			keys.add(String.valueOf(i));
		}
        //client.setOptimizeMergeBuffer(false);
		long start = System.nanoTime();
		Map<String, NameClass> result = null;
		for (int i = 0; i < repeat; i++)
			result = client.get(keys,1000);
		//19171967387
		System.out.println(System.nanoTime() - start);
		System.out.println(result.size());
		client.shutdown();
	}
}
