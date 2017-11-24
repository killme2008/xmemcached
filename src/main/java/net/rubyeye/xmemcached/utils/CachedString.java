package net.rubyeye.xmemcached.utils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedString {
	private static ConcurrentHashMap<String, Reference<byte[]>> table = new ConcurrentHashMap();
	static final ReferenceQueue rq = new ReferenceQueue();
	public static final int CLEAR_THRESHOLD = Integer.parseInt(
			System.getProperty("xmemcached.string.bytes.cached.clear_threshold",
					"1000"));

	public static byte[] getBytes(String s) {
		if (s == null || s.length() == 0) {
			return null;
		}
		byte[] bs = null;
		Reference<byte[]> existingRef = table.get(s);
		if (existingRef == null) {
			clearCache(rq, table);
			bs = s.getBytes(ByteUtils.DEFAULT_CHARSET);
			existingRef = table.putIfAbsent(s,
					new WeakReference<byte[]>(bs, rq));
		}
		if (existingRef == null) {
			return bs;
		}
		byte[] existingbs = existingRef.get();
		if (existingbs != null) {
			return existingbs;
		}
		// entry died in the interim, do over
		table.remove(s, existingRef);
		return getBytes(s);
	}

	static public <K, V> void clearCache(ReferenceQueue rq,
			ConcurrentHashMap<K, Reference<V>> cache) {
		if (cache.size() > CLEAR_THRESHOLD) {
			// cleanup any dead entries
			if (rq.poll() != null) {
				while (rq.poll() != null) {
					;
				}
				for (Map.Entry<K, Reference<V>> e : cache.entrySet()) {
					Reference<V> val = e.getValue();
					if (val != null && val.get() == null) {
						cache.remove(e.getKey(), val);
					}
				}
			}
		}
	}

	private static long testString(int keyLen) {
		String k = getKey(keyLen);
		long len = 0;
		for (int i = 0; i < 1000; i++) {
			// byte[] bs = k.getBytes(ByteUtils.DEFAULT_CHARSET);
			// String nk = new String(bs, ByteUtils.DEFAULT_CHARSET);
			byte[] bs = getBytes(k);
			String nk = ByteUtils.getString(bs);
			if (!k.equals(nk)) {
				throw new RuntimeException();
			}
			len += nk.length();
		}
		return len;
	}

	private static String getKey(int len) {
		StringBuilder sb = new StringBuilder();
		String[] chars = {"a", "b", "c", "d", "e", "f", "g", "h"};
		int index = (int) Math.floor(Math.random() * 8);
		for (int i = 0; i < len; i++) {
			sb.append(chars[index]);
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		long sum = 0;
		for (int i = 0; i < 10000; i++) {
			sum += testString(8);
		}
		int[] keys = {8, 64, 128};

		for (int k : keys) {
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {
				sum += testString(k);
			}
			System.out.println("Key length=" + k + ", cost "
					+ (System.currentTimeMillis() - start) + " ms.");
		}
		System.out.println(sum);
		System.out.println(table.size());
	}
}
