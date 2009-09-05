package net.rubyeye.xmemcached.impl;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientStateListener;

import com.google.code.yanf4j.core.Controller;
import com.google.code.yanf4j.core.ControllerStateListener;

/**
 * Adapte MemcachedClientStateListener to yanf4j's ControllStateListener
 * 
 * @author dennis
 * 
 */
public class MemcachedClientStateListenerAdapter implements
		ControllerStateListener {
	private final MemcachedClientStateListener memcachedClientStateListener;
	private final MemcachedClient memcachedClient;

	public MemcachedClientStateListenerAdapter(
			MemcachedClientStateListener memcachedClientStateListener,
			MemcachedClient memcachedClient) {
		super();
		this.memcachedClientStateListener = memcachedClientStateListener;
		this.memcachedClient = memcachedClient;
	}

	public final MemcachedClientStateListener getMemcachedClientStateListener() {
		return memcachedClientStateListener;
	}

	public final MemcachedClient getMemcachedClient() {
		return memcachedClient;
	}

	@Override
	public final void onAllSessionClosed(Controller acceptor) {

	}

	@Override
	public final void onException(Controller acceptor, Throwable t) {
		memcachedClientStateListener.onException(memcachedClient, t);

	}

	@Override
	public final void onReady(Controller acceptor) {

	}

	@Override
	public final void onStarted(Controller acceptor) {
		memcachedClientStateListener.onStarted(memcachedClient);

	}

	@Override
	public final void onStopped(Controller acceptor) {
		memcachedClientStateListener.onShutDown(memcachedClient);

	}

}
