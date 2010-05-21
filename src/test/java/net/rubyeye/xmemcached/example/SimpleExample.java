package net.rubyeye.xmemcached.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.KeyIterator;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * Simple example for xmemcached
 * 
 * @author boyan
 * 
 */
public class SimpleExample {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Useage:java SimpleExample [servers]");
			System.exit(1);
		}
		MemcachedClient memcachedClient = getMemcachedClient(args[0]);
		if (memcachedClient == null) {
			throw new NullPointerException(
					"Null MemcachedClient,please check memcached has been started");
		}
		try {
			Thread.sleep(1000);
			memcachedClient.set("hello", 0, "Hello,xmemcached");
			memcachedClient.set("hello2", 0, "Hello,xmemcached");
			String value = memcachedClient.get("hello");
			System.out.println("hello=" + value);

			memcachedClient.delete("hello");
			value = memcachedClient.get("hello");
			System.out.println("hello=" + value);

			// iterate all keys
			KeyIterator it = memcachedClient.getKeyIterator(AddrUtil
					.getOneAddress(args[0]));
			while (it.hasNext()) {
				System.out.println(it.next());
			}

		} catch (MemcachedException e) {
			System.err.println("MemcachedClient operation fail");
			e.printStackTrace();
		} catch (TimeoutException e) {
			System.err.println("MemcachedClient operation timeout");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// ignore
		}
		try {
			memcachedClient.shutdown();
		} catch (Exception e) {
			System.err.println("Shutdown MemcachedClient fail");
			e.printStackTrace();
		}
	}

	public static MemcachedClient getMemcachedClient(String servers) {
		try {
			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(servers));
			builder.getAuthInfoMap().put(
					new InetSocketAddress("192.168.207.140", 11211),
					AuthInfo.typical("cacheuser", "test"));
			builder.setCommandFactory(new BinaryCommandFactory());
			return builder.build();
		} catch (IOException e) {
			System.err.println("Create MemcachedClient fail");
			e.printStackTrace();
		}
		return null;
	}
}
