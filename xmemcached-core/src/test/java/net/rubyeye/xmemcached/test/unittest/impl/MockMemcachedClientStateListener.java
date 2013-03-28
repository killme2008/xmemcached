package net.rubyeye.xmemcached.test.unittest.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientStateListener;

public class MockMemcachedClientStateListener implements
		MemcachedClientStateListener {
	AtomicInteger num;

	public MockMemcachedClientStateListener() {
		this.num = new AtomicInteger(0);
	}

	
	public void onConnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress) {
		this.num.incrementAndGet();
		System.out.println("Connected to " + inetSocketAddress.getHostName()
				+ ":" + inetSocketAddress.getPort());
	}

	
	public void onDisconnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress) {
		this.num.incrementAndGet();
		System.out.println("Disconnected to " + inetSocketAddress.getHostName()
				+ ":" + inetSocketAddress.getPort());
	}

	
	public void onException(MemcachedClient memcachedClient, Throwable throwable) {
		this.num.incrementAndGet();
		System.out.println("Client onException");

	}

	public int getNum() {
		return this.num.get();
	}

	
	public void onShutDown(MemcachedClient memcachedClient) {
		this.num.incrementAndGet();
		System.out.println("Client shutdown");

	}

	
	public void onStarted(MemcachedClient memcachedClient) {
		this.num.incrementAndGet();
		System.out.println("Client started");
	}

}
