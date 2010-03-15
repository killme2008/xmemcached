package net.rubyeye.xmemcached.test.legacy;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.TokyoTyrantTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by IntelliJ IDEA. User: yanwt Date: 2009-10-16 Time: 10:54:20
 */
public class TokyoTyrantTest {

	static int threads = 100; // 运行的测试线程数

	static int runs = 10000; // 每个线程运行的次数

	static int size = 100; // 设置到memcache中的数据包大小，单位k

	static Integer myLock = 100;// 锁定以下计数器

	static long putTimes = 0; // put总时间，单位微秒

	static long getTimes = 0; // get总时间，单位微秒

	// private static MemcachedClient memcachedClient = null;
	public static void main(String[] args) throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses("localhost:12000"));
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		builder.setTranscoder(new TokyoTyrantTranscoder());
		builder.setConnectionPoolSize(1);
		MemcachedClient memcachedClient = builder.build();

		myLock = new Integer(threads);

		for (int i = 0; i < threads; i++) {
			Thread thread = new WorkerThread(memcachedClient);
			thread.start();
		}
	}

	private static class WorkerThread extends Thread {
		MemcachedClient memcachedClient = null;

		// 构造函数

		WorkerThread(MemcachedClient memcachedClient) {
			this.memcachedClient = memcachedClient;
		}

		public void run() {
			// get object to store
			/*
			 * int[] obj = new int[size]; for (int i = 0; i < size; i++) {
			 * obj[i] = i; }
			 */

			String obj = "这里使用 $memcache->addServer 而不是 $memcache->connect 去连接 Tokyo Tyrant 服务器，是因为当 Memcache 客户端使用 addServer 服务器池时，是根据“crc32(key) % current_server_num”哈希算法将 key 哈希到不同的服务器的，PHP、C 和 python 的客户端都是如此的算法。Memcache 客户端的 addserver 具有故障转移机制，当 addserver 了2台 Memcached 服务器，而其中1台宕机了，那么 current_server_num 会由原先的2变成1。 \n"
					+ "\n"
					+ "　　引用 memcached 官方网站和 PHP 手册中的两段话： \n"
					+ "\n"
					+ "http://www.danga.com/memcached/ \n"
					+ "If a host goes down, the API re-maps that dead host's requests onto the servers that are available. \n"
					+ "\n"
					+ "http://cn.php.net/manual/zh/function.Memcache-addServer.php \n"
					+ "Failover may occur at any stage in any of the methods, as long as other servers are available the request the user won't notice. Any kind of socket or Memcached server level errors (except out-of-memory) may trigger the failover. Normal client errors such as adding an existing key will not trigger a failover. ";
			String[] keys = new String[runs];
			for (int i = 0; i < runs; i++) {
				keys[i] = "test_key" + i;
			}

			for (int i = 0; i < runs; i++) {
				try {
					memcachedClient.delete(keys[i]);
				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (MemcachedException e) {
					e.printStackTrace();
				}
			}

			long startTime = System.currentTimeMillis();
			for (int i = 0; i < runs; i++) {
				try {
					memcachedClient.add(keys[i], 0, obj);

				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (MemcachedException e) {
					e.printStackTrace();
				}
			}
			long time = System.currentTimeMillis() - startTime;

			synchronized (myLock) {
				putTimes += time;
			}

			startTime = System.currentTimeMillis();
			for (int i = 0; i < runs; i++) {
				try {
					memcachedClient.get(keys[i]);
				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (MemcachedException e) {
					e.printStackTrace();
				}
			}
			time = System.currentTimeMillis() - startTime;

			synchronized (myLock) {
				getTimes += time;
				myLock--;

				if (myLock.equals(0)) {
					System.out
							.println("测试完成! 启动线程数:" + threads + ", 每线程执行测试数量: "
									+ runs + ", 测试数据大小(byte):" + size);

					System.out.println("put处理时间:" + putTimes
							+ "微秒，处理put速度: 每秒 " + runs * threads * 1000
							/ putTimes + " 次");
					System.out.println("get处理时间:" + getTimes
							+ "微秒，处理get速度: 每秒 " + runs * threads * 1000
							/ getTimes + " 次");
				}
			}
		}
	}
}
