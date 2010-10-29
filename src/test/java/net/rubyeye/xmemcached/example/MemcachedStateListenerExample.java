package net.rubyeye.xmemcached.example;

import java.io.IOException;
import java.net.InetSocketAddress;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedClientStateListener;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

class MyListener implements MemcachedClientStateListener {

	public void onConnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress) {
		System.out.println("Connect to " + inetSocketAddress);
	}

	public void onDisconnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress) {
		System.out.println("Disconnect from " + inetSocketAddress);

	}

	public void onException(MemcachedClient memcachedClient, Throwable throwable) {
		throwable.printStackTrace();

	}

	public void onShutDown(MemcachedClient memcachedClient) {
		System.out.println("MemcachedClient has been shutdown");

	}

	public void onStarted(MemcachedClient memcachedClient) {
		System.out.println("MemcachedClient has been started");

	}

}

public class MemcachedStateListenerExample {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err
					.println("Useage:java MemcachedStateListenerExample [servers]");
			System.exit(1);
		}
		MemcachedClient memcachedClient = getMemcachedClient(args[0]);
		try {
			memcachedClient.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static MemcachedClient getMemcachedClient(String servers) {
		try {
			// use text protocol by default
			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(servers));
			// Add my state listener
			builder.addStateListener(new MyListener());
			return builder.build();
		} catch (IOException e) {
			System.err.println("Create MemcachedClient fail");
			e.printStackTrace();
		}
		return null;
	}

}
