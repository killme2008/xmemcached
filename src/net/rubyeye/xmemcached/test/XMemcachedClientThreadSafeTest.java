package net.rubyeye.xmemcached.test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import net.rubyeye.xmemcached.XMemcachedClient;

import com.google.code.yanf4j.util.*;

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
				this.xmemcachedClient.set("test" + number + "" + i, 0, i);
			}
			for (int i = 0; i < NUM; i++) {
				assert ((Integer) this.xmemcachedClient.get("test" + number
						+ "" + i) == i);
			}
			for (int i = 0; i < NUM; i++) {
				assert (this.xmemcachedClient.delete("test" + number + "" + i));
			}
			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

public class XMemcachedClientThreadSafeTest {
	static int num = 10;

	public static void main(String args[]) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		// Queue<TestMessage> queue = new MessageQueue<TestMessage>(1024 * 1024,
		// 1024 * 1024);
		AtomicLong sum = new AtomicLong(0);
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
