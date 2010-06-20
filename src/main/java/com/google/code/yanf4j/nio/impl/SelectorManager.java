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
 * Selector��������������reactor������һ��reactor����accept������reactor��������IO�¼�
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:10:59
 */
public class SelectorManager {
	private final Reactor[] reactorSet;
	private final AtomicInteger sets = new AtomicInteger(0);
	private final NioController controller;
	private final int dividend;

	/**
	 * Reactor��������
	 */
	private int reactorReadyCount;

	public SelectorManager(int selectorPoolSize, NioController controller,
			Configuration conf) throws IOException {
		if (selectorPoolSize <= 0) {
			throw new IllegalArgumentException("selectorPoolSize<=0");
		}
		log.info("Creating " + selectorPoolSize + " rectors...");
		this.reactorSet = new Reactor[selectorPoolSize];
		this.controller = controller;
		// ����selectorPoolSize��reactor
		for (int i = 0; i < selectorPoolSize; i++) {
			this.reactorSet[i] = new Reactor(this, conf, i);
		}
		this.dividend = this.reactorSet.length - 1;
	}

	private volatile boolean started;

	public int getSelectorCount() {
		return this.reactorSet == null ? 0 : this.reactorSet.length;
	}

	public synchronized void start() {
		if (this.started) {
			return;
		}
		this.started = true;
		for (Reactor reactor : this.reactorSet) {
			reactor.start();
		}
	}

	/**
	 * ��������ȡreactor
	 * 
	 * @param index
	 * @return
	 */
	public Reactor getReactorByIndex(int index) {
		if (index < 0 || index > this.reactorSet.length - 1) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return this.reactorSet[index];
	}

	public synchronized void stop() {
		if (!this.started) {
			return;
		}
		this.started = false;
		for (Reactor reactor : this.reactorSet) {
			reactor.interrupt();
		}
	}

	public static final String REACTOR_ATTRIBUTE = System.currentTimeMillis()
			+ "_Reactor_Attribute";

	/**
	 * ע��channel
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
		// Accept����һ��reactor����
		if (ops == SelectionKey.OP_ACCEPT || ops == SelectionKey.OP_CONNECT) {
			index = 0;
		} else {
			index = this.sets.incrementAndGet() % this.dividend + 1;
		}
		final Reactor reactor = this.reactorSet[index];
		reactor.registerChannel(channel, ops, attachment);
		return reactor;

	}

	void awaitReady() {
		synchronized (this) {
			while (!this.started
					|| this.reactorReadyCount != this.reactorSet.length) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();// reset interrupt status
				}
			}
		}
	}

	/**
	 * ��һ��reactor
	 * 
	 * @return
	 */
	public final Reactor nextReactor() {
		return this.reactorSet[this.sets.incrementAndGet() % this.dividend + 1];
	}

	/**
	 * ע�������¼�
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
		return this.controller;
	}

	synchronized void notifyReady() {
		this.reactorReadyCount++;
		if (this.reactorReadyCount == this.reactorSet.length) {
			this.controller.notifyReady();
			this.notifyAll();
		}

	}

	private static final Logger log = LoggerFactory
			.getLogger(SelectorManager.class);

	public final boolean isStarted() {
		return this.started;
	}
}
