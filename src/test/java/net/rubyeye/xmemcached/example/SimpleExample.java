package net.rubyeye.xmemcached.example;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
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
		if (memcachedClient == null)
			throw new NullPointerException(
					"Null MemcachedClient,please check memcached has been started");
		try {
			memcachedClient.set("hello", 0, "Hello,xmemcached");

			String value = memcachedClient.get("hello");
			System.out.println("hello=" + value);

			memcachedClient.delete("hello");
			value = memcachedClient.get("hello");
			System.out.println("hello=" + value);

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
		} catch (IOException e) {
			System.err.println("Shutdown MemcachedClient fail");
			e.printStackTrace();
		}
	}

	public static MemcachedClient getMemcachedClient(String servers) {
		try {
			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(servers));
			return builder.build();
		} catch (IOException e) {
			System.err.println("Create MemcachedClient fail");
			e.printStackTrace();
		}
		return null;
	}
}
