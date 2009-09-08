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
		return this.memcachedClientStateListener;
	}

	public final MemcachedClient getMemcachedClient() {
		return this.memcachedClient;
	}

	
	public final void onAllSessionClosed(Controller acceptor) {

	}

	
	public final void onException(Controller acceptor, Throwable t) {
		this.memcachedClientStateListener.onException(this.memcachedClient, t);

	}

	
	public final void onReady(Controller acceptor) {

	}

	
	public final void onStarted(Controller acceptor) {
		this.memcachedClientStateListener.onStarted(this.memcachedClient);

	}

	
	public final void onStopped(Controller acceptor) {
		this.memcachedClientStateListener.onShutDown(this.memcachedClient);

	}

}
