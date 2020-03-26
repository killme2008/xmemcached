package net.rubyeye.memcached.benchmark;

public class StringGenerator {
	public static final String generate(int n, int length) {
		StringBuilder result = new StringBuilder(String.valueOf(n));
		while (result.length() < length) {
			result.append("0");
		}
		return result.toString();
	}
}
