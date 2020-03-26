package net.rubyeye.memcached.benchmark;

public interface Constants {

	final int[] THREADS = {
	// 200,
			1, 10, 50, 100, 300 };
	final int[] BYTES = {
	// 128,
			64, 512, 1024, 4096, 16 * 1024, };
	public static final double WRITE_RATE = 0.20;
	final int BASE_REPEATS = 40000;
	final long OP_TIMEOUT = 5000;

}
