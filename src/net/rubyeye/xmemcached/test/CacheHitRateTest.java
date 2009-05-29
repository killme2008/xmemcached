/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.test;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import com.google.code.yanf4j.util.ResourcesUtils;

/**
 * Count words from an artical("Golden Compass"),and store result to memcached.
 * Then remove or add memcached server,calculate the cache hit rate for
 * different hash strategy.
 * 
 * @author dennis
 * 
 */
public class CacheHitRateTest {

	public static void main(String[] args) throws Exception {
		String ip = "192.168.222.100";

		// Replace this hashAlg with other HashAlgorithm
		HashAlgorithm hashAlg = HashAlgorithm.KETAMA_HASH;

		MemcachedClient client = new XMemcachedClient(
				new KetamaMemcachedSessionLocator(hashAlg));
		client.addServer(ip, 12000);
		client.addServer(ip, 12001);
		client.addServer(ip, 12002);
		client.addServer(ip, 12003);
		client.addServer(ip, 12004);
		client.addServer(ip, 12005);
		client.addServer(ip, 12006);
		client.addServer(ip, 12007);
		client.addServer(ip, 12008);
		client.addServer(ip, 12009);
		// Count words,store result to memcached
		Set<String> keys = init(client);
		// Test different HashAlgorithm's performance
		testHashPerfromance(keys, HashAlgorithm.CRC32_HASH);
		testHashPerfromance(keys, HashAlgorithm.KETAMA_HASH);
		testHashPerfromance(keys, HashAlgorithm.FNV1_32_HASH);
		testHashPerfromance(keys, HashAlgorithm.NATIVE_HASH);
		testHashPerfromance(keys, HashAlgorithm.MYSQL_HASH);
		// It must be 100%
		printHitRate(client, keys);
		// Add two server,calculate the cache hit rate now
		client.addServer(ip, 12010);
		client.addServer(ip, 12011);
		printHitRate(client, keys);
		client.shutdown();
	}

	private static void testHashPerfromance(Set<String> keys,
			HashAlgorithm hashAlg) {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			for (String key : keys) {
				hashAlg.hash(key);
			}
		}
		System.out.println(hashAlg.name() + ":"
				+ (System.currentTimeMillis() - start));
	}

	private static void printHitRate(MemcachedClient client, Set<String> keys)
			throws TimeoutException, InterruptedException, MemcachedException {
		int total = 0;
		int hits = 0;
		for (String key : keys) {
			total++;
			if (client.get(key) != null)
				hits++;
		}
		System.out.println("hit rate=" + (double) hits / total);
	}

	/**
	 * 初始化,统计单词并存储到10个节点，单词统计并不严谨，只是为了让key比较随机
	 * 
	 * @param client
	 * @throws Exception
	 */
	public static Set<String> init(MemcachedClient client) throws Exception {
		BufferedReader reader = new BufferedReader(ResourcesUtils
				.getResourceAsReader("golden_compass.txt"));
		String line = null;
		Map<String, Integer> counters = new HashMap<String, Integer>();
		while ((line = reader.readLine()) != null) {
			String[] words = line.split("[\\s,.,\"'?\t]+");
			for (String word : words) {
				word = word.trim();
				if (word.length() == 0)
					continue;
				if (counters.containsKey(word)) {
					counters.put(word, counters.get(word).intValue() + 1);
				} else {
					counters.put(word, 1);
				}
			}
		}
		System.out.println("words number=" + counters.keySet().size());
		Iterator<Map.Entry<String, Integer>> it = counters.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, Integer> entry = it.next();
			if (!client.set(entry.getKey(), 0, entry.getValue()))
				System.err.println("put error");
		}
		System.out.println("initialize successfully!");
		reader.close();
		return counters.keySet();
	}

}
