package net.rubyeye.memcached.benchmark;

public interface Constants {

	final int[] THREADS = {
	// 200,
			1, 10, 50, 100, 200, 500 // , 1000
	};
	final int[] BYTES = {
	// 128,
			64, 128, 256, 512, 1024, 4096, 8192, 16 * 1024 };
	public static final double WRITE_RATE = 0.3;
	final int BASE_REPEATS = 10000;
	final long OP_TIMEOUT=5000;
}
