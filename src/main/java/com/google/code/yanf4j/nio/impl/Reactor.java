/**
 * 
 */

package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.util.LinkedTransferQueue;
import com.google.code.yanf4j.util.SystemUtils;

/**
 * 
 * 
 * Reactorģʽʵ��
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ����01:25:19
 */

public final class Reactor extends Thread {
	/**
	 * JVM bug����	
	 */
	public static final int JVMBUG_THRESHHOLD = Integer.getInteger(
			"com.googlecode.yanf4j.nio.JVMBUG_THRESHHOLD", 128);
	public static final int JVMBUG_THRESHHOLD2 = JVMBUG_THRESHHOLD * 2;
	public static final int JVMBUG_THRESHHOLD1 = (JVMBUG_THRESHHOLD2 + JVMBUG_THRESHHOLD) / 2;
	public static final int DEFAULT_WAIT = 1000;

	private static final Logger log = LoggerFactory.getLogger("remoting");

	private boolean jvmBug0;
	private boolean jvmBug1;

	private final int reactorIndex;

	private final SelectorManager selectorManager;

	private final AtomicInteger jvmBug = new AtomicInteger(0);

	private long lastJVMBug;

	private volatile Selector selector;

	private final NioController controller;

	private final Configuration configuration;

	private final AtomicBoolean wakenUp = new AtomicBoolean(false);

	private final Queue<Object[]> register = new LinkedTransferQueue<Object[]>();

	private final Lock gate = new ReentrantLock();

	private volatile int selectTries = 0;

	private long nextTimeout = 0;

	Reactor(SelectorManager selectorManager, Configuration configuration,
			int index) throws IOException {
		super();
		this.reactorIndex = index;
		this.selectorManager = selectorManager;
		this.controller = selectorManager.getController();
		this.selector = SystemUtils.openSelector();
		this.configuration = configuration;
	}

	public final Selector getSelector() {
		return this.selector;
	}

	public int getReactorIndex() {
		return this.reactorIndex;
	}

	@Override
	public void run() {
		this.selectorManager.notifyReady();
		while (this.selectorManager.isStarted() && this.selector.isOpen()) {
			try {
				beforeSelect();
				this.wakenUp.set(false);
				long before = -1;
				//�Ƿ���Ҫ�鿴jvm bug
				if (isNeedLookingJVMBug()) {
					before = System.currentTimeMillis();
				}
				long wait = DEFAULT_WAIT;
				if (this.nextTimeout > 0) {
					wait = this.nextTimeout;
				}
				int selected = this.selector.select(wait);
				if (selected == 0) {
					if (before != -1) {
						lookJVMBug(before, selected, wait);
					}
					this.selectTries++;
                    //��ȡ�´γ�ʱʱ��
					this.nextTimeout = checkSessionTimeout();
					continue;
				} else {
					this.selectTries = 0;
				}

			} catch (ClosedSelectorException e) {
				break;
			} catch (IOException e) {
				log.error("Reactor select error", e);
				if (this.selector.isOpen()) {
					continue;
				} else {
					break;
				}
			}
			Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
			this.gate.lock();
			try {
				postSelect(selectedKeys, this.selector.keys());
				dispatchEvent(selectedKeys);
			} finally {
				this.gate.unlock();
			}
		}
		if (this.selector != null) {
			if (this.selector.isOpen()) {
				try {
					this.controller.closeChannel(this.selector);
					this.selector.close();
				} catch (IOException e) {
					this.controller.notifyException(e);
					log.error("stop reactor error", e);
				}
			}
		}

	}

	/**
	 * ���JVM bug�Ƿ���
	 * @param before
	 * @param selected
	 * @param wait
	 * @return
	 * @throws IOException
	 */
	private boolean lookJVMBug(long before, int selected, long wait)
			throws IOException {
		boolean seeing = false;
		long now = System.currentTimeMillis();

		if (JVMBUG_THRESHHOLD > 0 && selected == 0 && wait > JVMBUG_THRESHHOLD
				&& now - before < wait / 4 && !this.wakenUp.get() /* waken up */
				&& !Thread.currentThread().isInterrupted()/* Interrupted */) {
			this.jvmBug.incrementAndGet();
			if (this.jvmBug.get() >= JVMBUG_THRESHHOLD2) {
				this.gate.lock();
				try {
					this.lastJVMBug = now;
					log
							.warn("JVM bug occured at "
									+ new Date(this.lastJVMBug)
									+ ",http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933,reactIndex="
									+ this.reactorIndex);
					if (this.jvmBug1) {
						log
								.debug("seeing JVM BUG(s) - recreating selector,reactIndex="
										+ this.reactorIndex);
					} else {
						this.jvmBug1 = true;
						log
								.info("seeing JVM BUG(s) - recreating selector,reactIndex="
										+ this.reactorIndex);
					}
					seeing = true;
					final Selector new_selector = SystemUtils.openSelector();

					for (SelectionKey k : this.selector.keys()) {
						if (!k.isValid() || k.interestOps() == 0) {
							continue;
						}

						final SelectableChannel channel = k.channel();
						final Object attachment = k.attachment();

						channel.register(new_selector, k.interestOps(),
								attachment);
					}

					this.selector.close();
					this.selector = new_selector;

				} finally {
					this.gate.unlock();
				}
				this.jvmBug.set(0);

			} else if (this.jvmBug.get() == JVMBUG_THRESHHOLD
					|| this.jvmBug.get() == JVMBUG_THRESHHOLD1) {
				if (this.jvmBug0) {
					log
							.debug("seeing JVM BUG(s) - cancelling interestOps==0,reactIndex="
									+ this.reactorIndex);
				} else {
					this.jvmBug0 = true;
					log
							.info("seeing JVM BUG(s) - cancelling interestOps==0,reactIndex="
									+ this.reactorIndex);
				}
				this.gate.lock();
				seeing = true;
				try {
					for (SelectionKey k : this.selector.keys()) {
						if (k.isValid() && k.interestOps() == 0) {
							k.cancel();
						}
					}
				} finally {
					this.gate.unlock();
				}
			}
		} else {
			this.jvmBug.set(0);
		}
		return seeing;
	}

	private boolean isNeedLookingJVMBug() {
		return SystemUtils.isLinuxPlatform()
				&& !SystemUtils.isAfterJava6u4Version();
	}

	/**
	 * �ɷ�IO�¼�
	 * @param selectedKeySet
	 */
	public final void dispatchEvent(Set<SelectionKey> selectedKeySet) {
		Iterator<SelectionKey> it = selectedKeySet.iterator();
		boolean skipOpRead = false;
		while (it.hasNext()) {
			SelectionKey key = it.next();
			it.remove();
			if (!key.isValid()) {
				if (key.attachment() != null) {
					this.controller.closeSelectionKey(key);
				} else {
					key.cancel();
				}
				continue;
			}
			try {
				if (key.isValid() && key.isAcceptable()) {
					this.controller.onAccept(key);
					continue;
				}
				if (key.isValid()
						&& (key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
					// Remove write interest
					key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
					this.controller.onWrite(key);
					if (!this.controller.isHandleReadWriteConcurrently()) {
						skipOpRead = true;
					}
				}
				if (!skipOpRead
						&& key.isValid()
						&& (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
					key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
					if (!this.controller.getStatistics().isReceiveOverFlow()) {
						// Remove read interest
						this.controller.onRead(key);
					} else {
						key.interestOps(key.interestOps()
								| SelectionKey.OP_READ);
					}

				}
				if ((key.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
					this.controller.onConnect(key);
				}

			} catch (RejectedExecutionException e) {

				if (key.attachment() instanceof AbstractNioSession) {
					((AbstractNioSession) key.attachment()).onException(e);
				}
				this.controller.notifyException(e);
				if (this.selector.isOpen()) {
					continue;
				} else {
					break;
				}
			} catch (Exception e) {
				if (key.attachment() instanceof AbstractNioSession) {
					((AbstractNioSession) key.attachment()).onException(e);
				}
				this.controller.closeSelectionKey(key);
				this.controller.notifyException(e);
				log.error("Reactor dispatch events error", e);
				if (this.selector.isOpen()) {
					continue;
				} else {
					break;
				}
			}
		}
	}

	final void unregisterChannel(SelectableChannel channel) throws IOException {
		Selector selector = this.selector;
		if (selector != null) {
			if (channel != null) {
				SelectionKey key = channel.keyFor(selector);
				if (key != null) {
					key.cancel();
				}
			}
		}
		wakeup();
	}

	/**
	 * ��������Ƿ���ڻ���idle
	 * @return
	 */
	private final long checkSessionTimeout() {
		long nextTimeout = 0;
		if (this.configuration.getCheckSessionTimeoutInterval() > 0) {
			this.gate.lock();
			try {
				if (this.selectTries * 1000 >= this.configuration
						.getCheckSessionTimeoutInterval()) {
					nextTimeout = this.configuration
							.getCheckSessionTimeoutInterval();
					for (SelectionKey key : this.selector.keys()) {

						if (key.attachment() != null) {
							long n = checkExpiredIdle(key,
									getSessionFromAttchment(key));
							nextTimeout = n < nextTimeout ? n : nextTimeout;
						}
					}
					this.selectTries = 0;
				}
			} finally {
				this.gate.unlock();
			}
		}
		return nextTimeout;
	}

	private final Session getSessionFromAttchment(SelectionKey key) {
		if (key.attachment() instanceof Session) {
			return (Session) key.attachment();
		}
		return null;
	}

	public final void registerSession(Session session, EventType event) {
		final Selector selector = this.selector;
		if (isReactorThread() && selector != null) {
			dispatchSessionEvent(session, event, selector);
		} else {
			this.register.offer(new Object[] { session, event });
			wakeup();
		}
	}

	private final boolean isReactorThread() {
		return Thread.currentThread() == this;
	}

	final void beforeSelect() {
		this.controller.checkStatisticsForRestart();
		processRegister();
	}

	private final void processRegister() {
		Object[] object = null;
		while ((object = this.register.poll()) != null) {
			switch (object.length) {
			case 2:
				dispatchSessionEvent((Session) object[0],
						(EventType) object[1], this.selector);
				break;
			case 3:
				registerChannelNow((SelectableChannel) object[0],
						(Integer) object[1], object[2], this.selector);
				break;
			}
		}
	}

	Configuration getConfiguration() {
		return this.configuration;
	}

	private final void dispatchSessionEvent(Session session, EventType event,
			Selector selector) {
		if (session.isClosed() && event != EventType.UNREGISTER) {
			return;
		}
		if (EventType.REGISTER.equals(event)) {
			this.controller.registerSession(session);
		} else if (EventType.UNREGISTER.equals(event)) {
			this.controller.unregisterSession(session);
		} else {
			((NioSession) session).onEvent(event, selector);
		}
	}

	public final void postSelect(Set<SelectionKey> selectedKeys,
			Set<SelectionKey> allKeys) {
		if (this.controller.getSessionTimeout() > 0
				|| this.controller.getSessionIdleTimeout() > 0) {
			for (SelectionKey key : allKeys) {

				if (!selectedKeys.contains(key)) {
					if (key.attachment() != null) {
						checkExpiredIdle(key, getSessionFromAttchment(key));
					}
				}
			}
		}
	}

	private long checkExpiredIdle(SelectionKey key, Session session) {
		if (session == null) {
			return 0;
		}
		long nextTimeout = 0;
		boolean expired = false;
		if (this.controller.getSessionTimeout() > 0) {
			expired = checkExpired(key, session);
			nextTimeout = this.controller.getSessionTimeout();
		}
		if (this.controller.getSessionIdleTimeout() > 0 && !expired) {
			checkIdle(session);
			nextTimeout = this.controller.getSessionIdleTimeout();
		}
		return nextTimeout;
	}

	private final void checkIdle(Session session) {
		if (this.controller.getSessionIdleTimeout() > 0) {
			if (session.isIdle()) {
				((NioSession) session).onEvent(EventType.IDLE, this.selector);
			}
		}
	}

	private final boolean checkExpired(SelectionKey key, Session session) {
		if (session != null && session.isExpired()) {
			((NioSession) session).onEvent(EventType.EXPIRED, this.selector);
			this.controller.closeSelectionKey(key);
			return true;
		}
		return false;
	}

	public final void registerChannel(SelectableChannel channel, int ops,
			Object attachment) {
		final Selector selector = this.selector;
		if (isReactorThread() && selector != null) {
			registerChannelNow(channel, ops, attachment, selector);
		} else {
			this.register.offer(new Object[] { channel, ops, attachment });
			wakeup();
		}

	}

	private void registerChannelNow(SelectableChannel channel, int ops,
			Object attachment, Selector selector) {
		this.gate.lock();
		try {
			if (channel.isOpen()) {
				channel.register(selector, ops, attachment);
			}
		} catch (ClosedChannelException e) {
			log.error("Register channel error", e);
			this.controller.notifyException(e);
		} finally {
			this.gate.unlock();
		}
	}

	final void wakeup() {
		if (this.wakenUp.compareAndSet(false, true)) {
			final Selector selector = this.selector;
			if (selector != null) {
				selector.wakeup();
			}
		}
	}

	final void selectNow() throws IOException {
		final Selector selector = this.selector;
		if (selector != null) {
			selector.selectNow();
		}
	}
}