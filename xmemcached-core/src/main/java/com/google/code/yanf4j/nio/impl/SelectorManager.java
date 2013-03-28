package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Session;

/**
 * Selector manager
 * 
 * @author dennis
 * 
 */
public class SelectorManager {
	private final Reactor[] reactorSet;
	private final AtomicInteger sets = new AtomicInteger(0);
	private final NioController controller;
	private final int dividend;

	/**
	 * Reactor count which are ready
	 */
	private int reactorReadyCount;

	public SelectorManager(int selectorPoolSize, NioController controller,
			Configuration conf) throws IOException {
		if (selectorPoolSize <= 0) {
			throw new IllegalArgumentException("selectorPoolSize<=0");
		}
		log.info("Creating " + selectorPoolSize + " reactors...");
		reactorSet = new Reactor[selectorPoolSize];
		this.controller = controller;
		for (int i = 0; i < selectorPoolSize; i++) {
			reactorSet[i] = new Reactor(this, conf, i);
		}
		dividend = reactorSet.length - 1;
	}

	private volatile boolean started;

	public int getSelectorCount() {
		return reactorSet == null ? 0 : reactorSet.length;
	}

	public synchronized void start() {
		if (started) {
			return;
		}
		started = true;
		for (Reactor reactor : reactorSet) {
			reactor.start();
		}
	}

	Reactor getReactorFromSession(Session session) {
		Reactor reactor = (Reactor) session.getAttribute(REACTOR_ATTRIBUTE);

		if (reactor == null) {
			reactor = nextReactor();
			final Reactor oldReactor = (Reactor) session.setAttributeIfAbsent(
					REACTOR_ATTRIBUTE, reactor);
			if (oldReactor != null) {
				reactor = oldReactor;
			}
		}
		return reactor;
	}

	/**
	 * Find reactor by index
	 * 
	 * @param index
	 * @return
	 */
	public Reactor getReactorByIndex(int index) {
		if (index < 0 || index > reactorSet.length - 1) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return reactorSet[index];
	}

	public synchronized void stop() {
		if (!started) {
			return;
		}
		started = false;
		for (Reactor reactor : reactorSet) {
			reactor.interrupt();
		}
	}

	public static final String REACTOR_ATTRIBUTE = System.currentTimeMillis()
			+ "_Reactor_Attribute";

	/**
	 * Register channel
	 * 
	 * @param channel
	 * @param ops
	 * @param attachment
	 * @return
	 */
	public final Reactor registerChannel(SelectableChannel channel, int ops,
			Object attachment) {
		awaitReady();
		int index = 0;
		// Accept event used index 0 reactor
		if (ops == SelectionKey.OP_ACCEPT || ops == SelectionKey.OP_CONNECT) {
			index = 0;
		} else {
			index = sets.incrementAndGet() % dividend + 1;
		}
		final Reactor reactor = reactorSet[index];
		reactor.registerChannel(channel, ops, attachment);
		return reactor;

	}

	void awaitReady() {
		synchronized (this) {
			while (!started || reactorReadyCount != reactorSet.length) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();// reset interrupt status
				}
			}
		}
	}

	/**
	 * Get next reactor
	 * 
	 * @return
	 */
	public final Reactor nextReactor() {
		if (dividend > 0) {
			return reactorSet[sets.incrementAndGet() % dividend + 1];
		} else {
			return reactorSet[0];
		}
	}

	/**
	 * Register session
	 * 
	 * @param session
	 * @param event
	 */
	public final void registerSession(Session session, EventType event) {
		if (session.isClosed() && event != EventType.UNREGISTER) {
			return;
		}
		Reactor reactor = (Reactor) session.getAttribute(REACTOR_ATTRIBUTE);

		if (reactor == null) {
			reactor = nextReactor();
			final Reactor oldReactor = (Reactor) session.setAttributeIfAbsent(
					REACTOR_ATTRIBUTE, reactor);
			if (oldReactor != null) {
				reactor = oldReactor;
			}
		}
		reactor.registerSession(session, event);
	}

	public NioController getController() {
		return controller;
	}

	/**
	 * Notify all reactor have been ready
	 */
	synchronized void notifyReady() {
		reactorReadyCount++;
		if (reactorReadyCount == reactorSet.length) {
			controller.notifyReady();
			notifyAll();
		}

	}

	private static final Logger log = LoggerFactory
			.getLogger(SelectorManager.class);

	public final boolean isStarted() {
		return started;
	}
}
