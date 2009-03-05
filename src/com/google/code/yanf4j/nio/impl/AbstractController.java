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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.ControllerStateListener;
import com.google.code.yanf4j.nio.Dispatcher;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.util.DispatcherFactory;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.statistics.impl.DefaultStatistics;
import com.google.code.yanf4j.statistics.impl.SimpleStatistics;
import com.google.code.yanf4j.util.ConcurrentHashSet;

/**
 * Controller抽象基类,提供了Reactor实现以及一些基础代码
 * 
 * @author dennis
 * 
 */
public abstract class AbstractController extends SessionFlowController {
	protected static final Log log = LogFactory
			.getLog(AbstractController.class);

	/**
	 * controller状态监听器列表
	 */
	protected CopyOnWriteArrayList<ControllerStateListener> stateListeners = new CopyOnWriteArrayList<ControllerStateListener>();

	/**
	 * 事件处理handler
	 */
	@SuppressWarnings("unchecked")
	protected Handler handler;

	protected Selector selector;

	protected SelectionKey selectionKey;

	/**
	 * 反应器
	 */
	protected Reactor reactor;

	/**
	 * 编解码工厂
	 */
	@SuppressWarnings("unchecked")
	protected CodecFactory codecFactory;

	protected volatile boolean started;

	protected int port;

	/**
	 * 本地绑定地址
	 */
	protected InetSocketAddress socketAddress;

	protected boolean reuseAddress;

	protected int receiveBufferSize;

	protected int readThreadCount;

	protected Configuration configuration;

	protected Dispatcher dispatcher;

	protected long sessionTimeout;

	protected volatile boolean handleReadWriteConcurrently = true;

	protected int timeout = 0;

	// session集合
	protected Set<Session> sessionSet = new ConcurrentHashSet<Session>();

	public long getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public int getSoTimeout() {
		return timeout;
	}

	/**
	 * 设置socket SO_TIMEOUT选项
	 * 
	 * @param timeout
	 */
	public void setSoTimeout(int timeout) {
		this.timeout = timeout;
	}

	public AbstractController() {
		this(new Configuration(), null, null);
	}

	public AbstractController(Configuration configuration) {
		this(configuration, null, null);

	}

	@SuppressWarnings("unchecked")
	public AbstractController(Configuration configuration,
			CodecFactory codecFactory) {
		this(configuration, null, codecFactory);
	}

	@SuppressWarnings("unchecked")
	public AbstractController(Configuration configuration, Handler handler,
			CodecFactory codecFactory) {
		init(configuration, handler, codecFactory);
	}

	/**
	 * 初始化
	 * 
	 * @param configuration
	 * @param handler
	 * @param codecFactory
	 */
	@SuppressWarnings("unchecked")
	private synchronized void init(Configuration configuration,
			Handler handler, CodecFactory codecFactory) {
		this.handler = handler;
		setCodecFactory(codecFactory);
		this.configuration = configuration;
		setPort(configuration.getPort());
		setReadThreadCount(configuration.getReadThreadCount());
		setReceiveBufferSize(configuration.getTcpRecvBufferSize());
		setReuseAddress(configuration.isReuseAddress());
		setHandleReadWriteConcurrently(configuration
				.isHandleReadWriteConcurrently());
		setSoTimeout(configuration.getSoTimeout());
		if (configuration.isStatisticsServer()) {
			this.statistics = new SimpleStatistics();
			this.statisticsInterval = configuration.getStatisticsInterval();

		} else {
			this.statistics = new DefaultStatistics();
			this.statisticsInterval = -1;
		}
		this.receivePacketRate = -1.0;
		started = false;
	}

	public InetSocketAddress getLocalSocketAddress() {
		return this.socketAddress;
	}

	/**
	 * 设置本地绑定地址
	 * 
	 * @param inetSocketAddress
	 */
	public void setLocalSocketAddress(InetSocketAddress inetSocketAddress) {
		if (this.started)
			throw new IllegalStateException();
		this.socketAddress = inetSocketAddress;
		this.port = this.socketAddress.getPort();
	}

	/**
	 * 处理OP_ACCEPT事件
	 * 
	 * @param selectionKey
	 */
	public void onAccept(SelectionKey sk) throws IOException {
		this.statistics.statisticsAccept();
	}

	/**
	 * 处理OP_CONNECT事件
	 * 
	 * @param selectionKey
	 */
	public void onConnect(SelectionKey key) throws IOException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public void open(int port, boolean reuseAddr, Handler handler)
			throws IOException {
		open(port, reuseAddr, handler, null);
	}

	@SuppressWarnings("unchecked")
	public synchronized void open(int port, boolean reuseAddr, Handler handler,
			CodecFactory codecFactory) throws IOException {
		if (port < 0)
			throw new IllegalArgumentException("port<0");
		this.handler = handler;
		this.codecFactory = codecFactory;
		this.port = port;
		this.socketAddress = new InetSocketAddress(this.port);
		this.reuseAddress = reuseAddr;
		this.start();
	}

	@SuppressWarnings("unchecked")
	public void open(int port, Handler handler) throws IOException {
		open(port, false, handler);
	}

	@SuppressWarnings("unchecked")
	public void open(int port, Handler handler, CodecFactory codecFactory)
			throws IOException {
		open(port, false, handler, codecFactory);
	}

	@SuppressWarnings("unchecked")
	public synchronized void open(InetSocketAddress inetSocketAddress,
			boolean reuseAddr, Handler handler, CodecFactory codecFactory)
			throws IOException {
		if (inetSocketAddress == null)
			throw new NullPointerException("inetSocketAddress is null");
		this.handler = handler;
		this.codecFactory = codecFactory;
		this.socketAddress = inetSocketAddress;
		this.port = this.socketAddress.getPort();
		this.reuseAddress = reuseAddr;
		this.start();
	}

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, boolean reuseAddr,
			Handler handler) throws IOException {
		open(inetSocketAddress, reuseAddr, handler, null);
	}

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, Handler handler)
			throws IOException {
		open(inetSocketAddress, false, handler);
	}

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, Handler handler,
			CodecFactory codecFactory) throws IOException {
		open(inetSocketAddress, false, handler, codecFactory);
	}

	@SuppressWarnings("unchecked")
	public void open(Configuration configuration, Handler handler)
			throws IOException {
		init(configuration, handler, null);
		this.start();
	}

	@SuppressWarnings("unchecked")
	public void open(Configuration configuration, Handler handler,
			CodecFactory codecFactory) throws IOException {
		init(configuration, handler, codecFactory);
		this.start();
	}

	public void addStateListener(ControllerStateListener listener) {
		if (started)
			throw new IllegalStateException("The Controller have started");
		this.stateListeners.add(listener);
	}

	public boolean isHandleReadWriteConcurrently() {
		return handleReadWriteConcurrently;
	}

	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	public int getReadThreadCount() {
		return readThreadCount;
	}

	public void setReadThreadCount(int readThreadCount) {
		if (started)
			throw new IllegalStateException();
		if (readThreadCount < 0)
			throw new IllegalArgumentException();
		this.readThreadCount = readThreadCount;
	}

	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * 设置socket接收缓冲区大小
	 * 
	 * @param receiveBufferSize
	 */
	public void setReceiveBufferSize(int receiveBufferSize) {
		if (receiveBufferSize <= 0)
			throw new IllegalArgumentException("receiveBufferSize <=0");
		if (started && receiveBufferSize >= 64 * 1024)
			throw new IllegalStateException("The Controller have started");
		this.receiveBufferSize = receiveBufferSize;
	}

	@SuppressWarnings("unchecked")
	public Handler getHandler() {
		return handler;
	}

	@SuppressWarnings("unchecked")
	public void setHandler(Handler handler) {
		if (started)
			throw new IllegalStateException("The Controller have started");
		this.handler = handler;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		if (started)
			throw new IllegalStateException("The Controller have started");
		if (port < 0)
			throw new IllegalArgumentException("port<0");
		this.port = port;
		this.socketAddress = new InetSocketAddress(this.port);
	}

	public boolean isReuseAddress() {
		return reuseAddress;
	}

	public void setReuseAddress(boolean reuseAddress) {
		if (started)
			throw new IllegalStateException("The Controller have started");
		this.reuseAddress = reuseAddress;
	}

	/**
	 * 启动controller,通常你可能会使用open方法，open系列方法最后会调用start()方法
	 */
	public synchronized void start() throws IOException {
		if (started)
			return;
		if (handler == null)
			throw new IllegalStateException("The handler is null");
		if (getCodecFactory() == null)
			setCodecFactory(new ByteBufferCodecFactory());
		started = true;
		this.dispatcher = DispatcherFactory.newDispatcher(this.readThreadCount);
		try {
			selector = Selector.open();
			doStart();
		} catch (IOException e) {
			log.error("Start server error", e);
			notifyException(e);
			stop();
			throw e;
		}
		initializeReactor();
		this.statistics.start();
		notifyStarted();
		log.warn("The Controller started at port " + port + " ...");
	}

	protected void initializeReactor() {
		if (this.reactor == null) {
			this.reactor = new Reactor(this.selector, this.selectionKey, this);
			reactor.start();
		}
	}

	/**
	 * 实际的启动操作
	 * 
	 * @throws IOException
	 */
	protected abstract void doStart() throws IOException;

	/**
	 * 返回处理OP_READ的handler
	 * 
	 * @param key
	 * @return
	 */
	protected abstract Runnable getReadHandler(final SelectionKey key);

	public void onRead(SelectionKey key) {
		dispatcher.dispatch(getReadHandler(key));
	}

	public void cancelKey(SelectionKey key) {
		Session session = (Session) key.attachment();
		if (session != null)
			session.close();
	}

	/**
	 * 唤醒reactor
	 */
	public synchronized void wakeup() {
		this.reactor.wakeup();
	}

	/**
	 * 将session注册到eventType，并唤醒reactor触发
	 */
	public synchronized void wakeup(Session session, EventType eventType) {
		this.reactor.wakeup(session, eventType);
	}

	public void notifyStarted() {
		for (ControllerStateListener stateListener : stateListeners) {
			stateListener.onStarted(this);
		}
	}

	public boolean isStarted() {
		return started;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	@SuppressWarnings("unchecked")
	public synchronized CodecFactory getCodecFactory() {
		return codecFactory;
	}

	@SuppressWarnings("unchecked")
	public synchronized void setCodecFactory(CodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	public void notifyReady() {
		for (ControllerStateListener stateListener : stateListeners) {
			stateListener.onReady(this);
		}
	}

	public void unregisterSession(Session session) {
		sessionSet.remove(session);
		if (sessionSet.size() == 0)
			notifyAllSessionClosed();

	}

	public void checkStatisticsForRestart() {
		if (statisticsInterval > 0
				&& System.currentTimeMillis() - statistics.getStartedTime() > statisticsInterval * 1000) {
			statistics.restart();
		}
	}

	public void registerSession(Session session) {
		sessionSet.add(session);
	}

	protected boolean isReactorThread() {
		return Thread.currentThread() == reactor;
	}

	/**
	 * 停止controller
	 */
	public synchronized void stop() {
		if (!started)
			return;
		started = false;
		try {
			this.statistics.stop();
			this.dispatcher.close();
		} catch (IOException e) {
			notifyException(e);
			log.error("Shut down dispatcher error", e);
		}
		for (Session session : this.sessionSet) {
			session.close();
		}
		reactor.close();
		if (!isReactorThread()) { // Wait until finished
			while (reactor.isAlive()) {
				try {
					reactor.join(2000);
				} catch (InterruptedException e) {
				}
			}
		}
		notifyStopped();
		this.stateListeners.clear();
	}

	public void notifyException(Throwable t) {
		for (ControllerStateListener stateListener : stateListeners) {
			stateListener.onException(this, t);
		}
	}

	public void notifyStopped() {
		for (ControllerStateListener stateListener : stateListeners) {
			stateListener.onStopped(this);
		}
	}

	public void notifyAllSessionClosed() {
		for (ControllerStateListener stateListener : stateListeners) {
			stateListener.onAllSessionClosed(this);
		}
	}

	public Set<Session> getSessionSet() {
		return Collections.unmodifiableSet(sessionSet);
	}

	public synchronized Reactor getReactor() {
		return this.reactor;
	}

}
