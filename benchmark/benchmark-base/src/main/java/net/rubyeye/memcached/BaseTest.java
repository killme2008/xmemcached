package net.rubyeye.memcached;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

public class BaseTest implements Constants {

	private static Logger logger = LoggerFactory.getLogger("benchmark");

	public static void printHeader() {
		logger.info("threads,repeats,valueLength,tps,miss,fail,hit,all,hitRate");
	}

	public static void printResult(int length, int threads, int repeats,
			AtomicLong miss, AtomicLong fail, AtomicLong hit, long duration,
			long total) {
		DecimalFormat df = new DecimalFormat("######0.00");
		double hitRate = ((double) hit.get())
				/ (hit.get() + miss.get() + fail.get());
		logger.info(threads + ","  + repeats
				+ "," + length + "," + total * 1000000000
				/ duration + "," + miss.get() + "," + fail.get()
				+ "," + hit.get() + "," + total + ","
				+ df.format(hitRate));
	}

	protected static int getReapts(int i) {
		// int t = (int) Math.log(THREADS[i]);
		// int repeats = BASE_REPEATS * (t <= 0 ? 1 : t);
		// return repeats;
		return BASE_REPEATS;
	}

}
