package net.rubyeye.xmemcached.test;

import java.util.concurrent.*;
import net.rubyeye.xmemcached.XMemcachedClient;



class TestThread implements Runnable {
	XMemcachedClient xmemcachedClient;
	CyclicBarrier barrier;

	int number;

	final static int NUM = 1000;

	public TestThread(int number, XMemcachedClient xmemcachedClient,
			CyclicBarrier barrier) {
		this.xmemcachedClient = xmemcachedClient;
		this.number = number;
		this.barrier = barrier;
	}

	public void run() {
		try {
			barrier.await();
			for (int i = 0; i < NUM; i++) {
				this.xmemcachedClient.set("test_" + number + "_" + i, 0, i);
			}
			for (int i = 0; i < NUM; i++) {
				assert ((Integer) this.xmemcachedClient.get("test_" + number
						+ "_" + i) == i);
			}
			for (int i = 0; i < NUM; i++) {
				assert (this.xmemcachedClient.delete("test_" + number + "_" + i));
			}
			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(this.number+" "+e.getMessage());
		}
	}
}

public class XMemcachedClientThreadSafeTest {
	static int num = 200;

	public static void main(String args[]) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		XMemcachedClient client = new XMemcachedClient("192.168.222.100", 11211);
		for (int i = 0; i < num; i++)
			new Thread(new TestThread(i, client, barrier)).start();
		long start = System.currentTimeMillis();
		barrier.await();

		barrier.await();
		client.shutdown();
		System.out.println("timed:" + (System.currentTimeMillis() - start));
	}
}
