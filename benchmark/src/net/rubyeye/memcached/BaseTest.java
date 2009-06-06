package net.rubyeye.memcached;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.memcached.benchmark.Constants;

public class BaseTest {

	public static void printResult(int length, int threads, int repeats,
			AtomicLong miss, AtomicLong fail, AtomicLong hit, long duration,
			long total) {
		DecimalFormat df = new DecimalFormat("######0.00");
		System.out.println("threads="
				+ threads
				+ ",repeats="
				+ repeats
				+ ",valueLength="
				+ length
				+ ",tps="
				+ total
				* 1000000000
				/ duration
				+ ",miss="
				+ miss.get()
				+ ",fail="
				+ fail.get()
				+ ",hit="
				+ hit.get()
				+ ",all="
				+ total
				+ ",hitRate="
				+ df.format(((double) hit.get()) / total
						/ (1.0 - Constants.WRITE_RATE)));
	}

}
