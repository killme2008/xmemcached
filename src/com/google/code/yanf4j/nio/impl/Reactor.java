package com.google.code.yanf4j.nio.impl;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.channels.Selector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.util.EventType;

/**
 * Reactor模式，封装select，并派发事件
 * 
 * @author dennis
 * 
 */
public class Reactor extends Thread implements SessionEventManager {
	protected static final Log log = LogFactory.getLog(Reactor.class);

	protected Selector selector;

	protected ControllerWrapper controller;

	protected volatile boolean started;

	protected SelectionKey sk;

	// 等待通知的session队列
	protected BlockingQueue<NotificationSession> waitForNotificationSessions = new LinkedBlockingQueue<NotificationSession>();

	/**
	 * 待注册事件列表
	 */
	@SuppressWarnings("unchecked")
	protected final List register = new LinkedList();

	protected volatile int selectTries = 0;

	public Reactor(Selector selector, SelectionKey sk,
			ControllerWrapper controller) {
		super();
		this.selector = selector;
		this.controller = controller;
		this.sk = sk;
		this.started = false;
	}

	@Override
	public synchronized void start() {
		this.started = true;
		super.start();
	}

	/**
	 * 触发后调用的方法，用户连接
	 */
	public void run() {
		controller.notifyReady();
		while (started && selector.isOpen()) {
			beforeSelect();
			try {
				// 阻塞
				if (selector.select(1000) == 0) {
					selectTries++;
					// 一定次数内没有select到任何key的时候，检测所有的key是否过期
					checkSessionTimeout();
					continue;
				} else {
					selectTries = 0;
				}
			} catch (ClosedSelectorException e) {
				break;
			} catch (IOException e) {
				log.error(e, e);
				if (selector.isOpen())
					continue;
				else
					break;
			}
			postSelect();
			Set<SelectionKey> selectedKeySet = selector.selectedKeys();
			if (selectedKeySet.size() > 0) {
				Iterator<SelectionKey> it = selectedKeySet.iterator();
				boolean skipOpRead = false; // 是否跳过读操作
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					if (!key.isValid()) {
						if (key.attachment() != null)
							controller.cancelKey(key);
						continue;
					}
					try {
						if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
							controller.onAccept(key);
							continue;
						}
						if (key.isValid()
								&& (key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
							controller.onWrite(key);
							if (!controller.isHandleReadWriteConcurrently()) {
								skipOpRead = true;
							}
							continue;
						}
						if (!skipOpRead
								&& (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

							// 移除读事件
							key.interestOps(key.interestOps()
									& ~SelectionKey.OP_READ);
							if (!controller.isOverFlow()) {
								controller.onRead(key);// 未超过控制流量，继续读
								continue;
							} else if (key.isValid())
								key.interestOps(key.interestOps() // 继续注册读事件
										| SelectionKey.OP_READ);

						}
						if ((key.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
							controller.onConnect(key);
							continue;
						}

					} catch (Exception e) {
						controller.cancelKey(key);
						controller.notifyException(e);
						log.error(e, e);
						if (selector.isOpen())
							continue;
						else
							break;
					}
				}
			}
		}
		close();

	}

	private void checkSessionTimeout() {
		if (selectTries * 1000 >= Configuration.CHECK_SESSION_TIMEOUT_INTERVAL) {
			for (SelectionKey key : selector.keys()) {
				// 检测是否超时
				if (key.attachment() != null) {
					Session session = (Session) key.attachment();
					if (!checkExpired(key, session))
						checkIdle(session);
				}
			}
			selectTries = 0;
		}
	}

	void close() {
		if (!this.started)
			return;
		this.started = false;
		if (selector != null) {
			try {
				for (SelectionKey key : selector.keys()) {
					if (key.attachment() != null) {
						((Session) key.attachment()).close();
					}
				}
				selector.selectNow();
			} catch (IOException e) {
				log.error("close dispatcher error", e);
			}
			if (selector.isOpen()) {
				try {
					if (sk != null)
						sk.cancel();
					controller.closeChannel();
					if (selector != null)
						selector.close();
				} catch (IOException e) {
					controller.notifyException(e);
					log.error("stop reactor error", e);
				}
				log.warn("Stopped reactor");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void register(Session session, EventType event) {
		if (isReactorThread()) {
			dispatchEvent(session, event);
		} else {
			synchronized (register) {
				register.add(new Object[] { session, event }); // 稍候再注册，通过beforeSelect方法
			}
			selector.wakeup();
		}
	}

	private boolean isReactorThread() {
		return Thread.currentThread() == this;
	}

	@SuppressWarnings("unchecked")
	protected void beforeSelect() {
		controller.checkStatisticsForRestart();
		processRegister();
		processNotify();
	}

	private void processNotify() {
		Iterator<NotificationSession> it = waitForNotificationSessions
				.iterator();
		while (it.hasNext()) {
			NotificationSession notifycationSession = it.next();
			it.remove();
			dispatchEvent(notifycationSession.session,
					notifycationSession.eventType);
		}
	}

	@SuppressWarnings("unchecked")
	private void processRegister() {
		Object[] objects = null;
		synchronized (register) {
			objects = new Object[register.size()];
			register.toArray(objects);
			register.clear();
		}
		for (int i = 0; i < objects.length; i++) {
			Object[] object = (Object[]) objects[i];
			dispatchEvent((Session) object[0], (EventType) object[1]);
		}
	}

	public void dispatchEvent(Session session, EventType event) {
		if (EventType.REGISTER.equals(event))
			controller.registerSession(session);
		else if (EventType.UNREGISTER.equals(event))
			controller.unregisterSession(session);
		else
			session.onEvent(event, selector);
	}

	protected void postSelect() {
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		for (SelectionKey key : selector.keys()) {
			// 没有在选择列表里面的key，检测是否超时
			if (!selectedKeys.contains(key)) {
				if (key.attachment() != null) {
					Session session = (Session) key.attachment();
					if (!checkExpired(key, session))
						checkIdle(session);
				}
			}
		}
	}

	private void checkIdle(Session session) {
		if (session instanceof DefaultTCPSession) {
			if (((DefaultTCPSession) session).isIdle()) {
				session.onEvent(EventType.IDLE, selector);
			}
		}
	}

	private boolean checkExpired(SelectionKey key, Session session) {
		if (session.isExpired()) {
			session.onEvent(EventType.EXPIRED, selector);
			controller.cancelKey(key);
			return true;
		}
		return false;
	}

	static class NotificationSession {
		Session session;
		EventType eventType;

		public NotificationSession(Session session, EventType eventType) {
			super();
			this.session = session;
			this.eventType = eventType;
		}
	}

	public void wakeup() {
		if (this.started)
			this.selector.wakeup();
	}

	public void wakeup(Session session, EventType eventType) {
		if (!this.started)
			return;
		this.waitForNotificationSessions.add(new NotificationSession(session,
				eventType));
		wakeup();
	}
}
