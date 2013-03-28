package net.rubyeye.xmemcached.example;

/**
 * Memcached using sasl for authentication
 * 
 * @author dennis
 * 
 */
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;

/**
 * Simple example for xmemcached,use binary protocol
 * 
 * @author boyan
 * 
 */
public class SASLExample {
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err
					.println("Useage:java SASLExample servers username password");
			System.exit(1);
		}
		MemcachedClient memcachedClient = getMemcachedClient(args[0], args[1],
				args[2]);
		if (memcachedClient == null) {
			throw new NullPointerException(
					"Null MemcachedClient,please check memcached has been started");
		}
		try {
			// add a
			System.out.println("Add a,b,c");
			memcachedClient.set("a", 0, "Hello,xmemcached");
			// get a
			String value = memcachedClient.get("a");
			System.out.println("get a=" + value);
			System.out.println("delete a");
			// delete a
			memcachedClient.delete("a");
			value = memcachedClient.get("a");
			System.out.println("after delete,a=" + value);
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

	public static MemcachedClient getMemcachedClient(String servers,
			String username, String password) {
		try {
			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(servers));
			builder.addAuthInfo(AddrUtil.getOneAddress(servers), AuthInfo
					.typical(username, password));
			// Must use binary protocol
			builder.setCommandFactory(new BinaryCommandFactory());
			return builder.build();
		} catch (IOException e) {
			System.err.println("Create MemcachedClient fail");
			e.printStackTrace();
		}
		return null;
	}
}
