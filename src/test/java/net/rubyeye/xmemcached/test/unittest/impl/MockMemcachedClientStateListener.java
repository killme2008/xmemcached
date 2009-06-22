package net.rubyeye.xmemcached.test.unittest.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientStateListener;

public class MockMemcachedClientStateListener implements
		MemcachedClientStateListener {
	AtomicInteger num;

	public MockMemcachedClientStateListener() {
		num = new AtomicInteger(0);
	}

	@Override
	public void onConnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress) {
		num.incrementAndGet();
		System.out.println("Connected to " + inetSocketAddress.getHostName()
				+ ":" + inetSocketAddress.getPort());
	}

	@Override
	public void onDisconnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress) {
		num.incrementAndGet();
		System.out.println("Disconnected to " + inetSocketAddress.getHostName()
				+ ":" + inetSocketAddress.getPort());
	}

	@Override
	public void onException(MemcachedClient memcachedClient, Throwable throwable) {
		num.incrementAndGet();
		System.out.println("Client onException");

	}

	public int getNum() {
		return num.get();
	}

	@Override
	public void onShutDown(MemcachedClient memcachedClient) {
		num.incrementAndGet();
		System.out.println("Client shutdown");

	}

	@Override
	public void onStarted(MemcachedClient memcachedClient) {
		num.incrementAndGet();
		System.out.println("Client started");
	}

}
