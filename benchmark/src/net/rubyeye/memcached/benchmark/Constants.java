package net.rubyeye.memcached.benchmark;

public interface Constants {

	final int[] THREADS = {
	// 200,
			1, 10, 50, 100, 150 // , 1000
	};
	final int[] BYTES = {
	// 128,
			64, 128, 512, 1024, 4096, 8192 };
	public static final double WRITE_RATE = 0.25;
	final int BASE_REPEATS = 20000;
	final long OP_TIMEOUT = 3000;

}
