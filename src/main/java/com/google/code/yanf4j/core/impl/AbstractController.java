/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.core.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Controller;
import com.google.code.yanf4j.core.ControllerLifeCycle;
import com.google.code.yanf4j.core.ControllerStateListener;
import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.statistics.impl.DefaultStatistics;
import com.google.code.yanf4j.statistics.impl.SimpleStatistics;
import com.google.code.yanf4j.util.DispatcherFactory;
import com.google.code.yanf4j.util.LinkedTransferQueue;

/**
 * Base controller
 * 
 * @author dennis
 * 
 */
public abstract class AbstractController implements Controller,
		ControllerLifeCycle {

	protected Statistics statistics = new DefaultStatistics();
	protected long statisticsInterval;

	protected static final Logger log = LoggerFactory
			.getLogger(AbstractController.class);
	/**
	 * controller state listener list
	 */
	protected CopyOnWriteArrayList<ControllerStateListener> stateListeners = new CopyOnWriteArrayList<ControllerStateListener>();
	/**
	 * Event handler
	 */
	protected Handler handler;
	/**
	 * Codec Factory
	 */

	protected volatile CodecFactory codecFactory;
	/**
	 * Status
	 */
	protected volatile boolean started;
	/**
	 * local bind address
	 */
	protected InetSocketAddress localSocketAddress;
	/**
	 * Read event processing thread count
	 */
	protected int readThreadCount;
	protected int writeThreadCount;
	protected int dispatchMessageThreadCount;
	protected Configuration configuration;
	protected Dispatcher readEventDispatcher, dispatchMessageDispatcher,
			writeEventDispatcher;
	protected long sessionTimeout;
	protected volatile boolean handleReadWriteConcurrently = true;

	protected int soTimeout;

	/**
	 * Socket options
	 */
	@SuppressWarnings("unchecked")
	protected Map<SocketOption, Object> socketOptions = new HashMap<SocketOption, Object>();

	@SuppressWarnings("unchecked")
	public void setSocketOptions(Map<SocketOption, Object> socketOptions) {
		if (socketOptions == null) {
			throw new NullPointerException("Null socketOptions");
		}
		this.socketOptions = socketOptions;
	}

	/**
	 * Connected session set
	 */
	private Set<Session> sessionSet = new HashSet<Session>();

	public final int getDispatchMessageThreadCount() {
		return this.dispatchMessageThreadCount;
	}

	public final void setDispatchMessageThreadCount(
			int dispatchMessageThreadPoolSize) {
		if (this.started) {
			throw new IllegalStateException("Controller is started");
		}
		if (dispatchMessageThreadPoolSize < 0) {
			throw new IllegalArgumentException(
					"dispatchMessageThreadPoolSize<0");
		}
		this.dispatchMessageThreadCount = dispatchMessageThreadPoolSize;
	}

	public long getSessionIdleTimeout() {
		return this.configuration.getSessionIdleTimeout();
	}

	/**
	 * Build write queue for session
	 * 
	 * @return
	 */
	protected Queue<WriteMessage> buildQueue() {
		return new LinkedTransferQueue<WriteMessage>();
	}

	public void setSessionIdleTimeout(long sessionIdleTimeout) {
		this.configuration.setSessionIdleTimeout(sessionIdleTimeout);

	}

	public long getSessionTimeout() {
		return this.sessionTimeout;
	}

	public void setSessionTimeout(long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public int getSoTimeout() {
		return this.soTimeout;
	}

	public void setSoTimeout(int timeout) {
		this.soTimeout = timeout;
	}

	public AbstractController() {
		this(new Configuration(), null, null);
	}

	public double getReceiveThroughputLimit() {
		return this.statistics.getReceiveThroughputLimit();
	}

	public double getSendThroughputLimit() {
		return this.statistics.getSendThroughputLimit();
	}

	public void setReceiveThroughputLimit(double receiveThroughputLimit) {
		this.statistics.setReceiveThroughputLimit(receiveThroughputLimit);

	}

	public void setSendThroughputLimit(double sendThroughputLimit) {
		this.statistics.setSendThroughputLimit(sendThroughputLimit);
	}

	public AbstractController(Configuration configuration) {
		this(configuration, null, null);

	}

	public AbstractController(Configuration configuration,
			CodecFactory codecFactory) {
		this(configuration, null, codecFactory);
	}

	public AbstractController(Configuration configuration, Handler handler,
			CodecFactory codecFactory) {
		init(configuration, handler, codecFactory);
	}

	private synchronized void init(Configuration configuration,
			Handler handler, CodecFactory codecFactory) {
		setHandler(handler);
		setCodecFactory(codecFactory);
		setConfiguration(configuration);
		setReadThreadCount(configuration.getReadThreadCount());
		setWriteThreadCount(configuration.getWriteThreadCount());
		setDispatchMessageThreadCount(configuration
				.getDispatchMessageThreadCount());
		setHandleReadWriteConcurrently(configuration
				.isHandleReadWriteConcurrently());
		setSoTimeout(configuration.getSoTimeout());
		setStatisticsConfig(configuration);
		setReceiveThroughputLimit(-0.1d);
		setStarted(false);
	}

	void setStarted(boolean started) {
		this.started = started;
	}

	private void setStatisticsConfig(Configuration configuration) {
		if (configuration.isStatisticsServer()) {
			this.statistics = new SimpleStatistics();
			this.statisticsInterval = configuration.getStatisticsInterval();

		} else {
			this.statistics = new DefaultStatistics();
			this.statisticsInterval = -1;
		}
	}

	public Configuration getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(Configuration configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException("Null Configuration");
		}
		this.configuration = configuration;
	}

	public InetSocketAddress getLocalSocketAddress() {
		return this.localSocketAddress;
	}

	public void setLocalSocketAddress(InetSocketAddress inetSocketAddress) {
		this.localSocketAddress = inetSocketAddress;
	}

	public void onAccept(SelectionKey sk) throws IOException {
		this.statistics.statisticsAccept();
	}

	public void onConnect(SelectionKey key) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void addStateListener(ControllerStateListener listener) {
		this.stateListeners.add(listener);
	}

	public void removeStateListener(ControllerStateListener listener) {
		this.stateListeners.remove(listener);
	}

	public boolean isHandleReadWriteConcurrently() {
		return this.handleReadWriteConcurrently;
	}

	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	public int getReadThreadCount() {
		return this.readThreadCount;
	}

	public void setReadThreadCount(int readThreadCount) {
		if (this.started) {
			throw new IllegalStateException();
		}
		if (readThreadCount < 0) {
			throw new IllegalArgumentException("readThreadCount<0");
		}
		this.readThreadCount = readThreadCount;
	}

	public final int getWriteThreadCount() {
		return this.writeThreadCount;
	}

	public final void setWriteThreadCount(int writeThreadCount) {
		if (this.started) {
			throw new IllegalStateException();
		}
		if (writeThreadCount < 0) {
			throw new IllegalArgumentException("readThreadCount<0");
		}
		this.writeThreadCount = writeThreadCount;
	}

	public Handler getHandler() {
		return this.handler;
	}

	public void setHandler(Handler handler) {
		if (this.started) {
			throw new IllegalStateException("The Controller have started");
		}
		this.handler = handler;
	}

	public int getPort() {
		if (this.localSocketAddress != null) {
			return this.localSocketAddress.getPort();
		}
		throw new NullPointerException("Controller is not binded");
	}

	public synchronized void start() throws IOException {
		if (isStarted()) {
			return;
		}
		if (getHandler() == null) {
			throw new IOException("The handler is null");
		}
		if (getCodecFactory() == null) {
			setCodecFactory(new ByteBufferCodecFactory());
		}
		setStarted(true);
		setReadEventDispatcher(DispatcherFactory.newDispatcher(
				getReadThreadCount(),
				new ThreadPoolExecutor.CallerRunsPolicy(),
				"xmemcached-read-thread"));
		setWriteEventDispatcher(DispatcherFactory.newDispatcher(
				getWriteThreadCount(),
				new ThreadPoolExecutor.CallerRunsPolicy(),
				"xmemcached-write-thread"));
		setDispatchMessageDispatcher(DispatcherFactory.newDispatcher(
				getDispatchMessageThreadCount(),
				new ThreadPoolExecutor.CallerRunsPolicy(),
				"xmemcached-dispatch-thread"));
		startStatistics();
		start0();
		notifyStarted();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					AbstractController.this.stop();
				} catch (IOException e) {
					log.error("Stop controller fail", e);
				}
			}
		});
		log.warn("The Controller started at " + this.localSocketAddress
				+ " ...");
	}

	protected abstract void start0() throws IOException;

	void setDispatchMessageDispatcher(Dispatcher dispatcher) {
		Dispatcher oldDispatcher = this.dispatchMessageDispatcher;
		this.dispatchMessageDispatcher = dispatcher;
		if (oldDispatcher != null) {
			oldDispatcher.stop();
		}
	}

	Dispatcher getReadEventDispatcher() {
		return this.readEventDispatcher;
	}

	void setReadEventDispatcher(Dispatcher dispatcher) {
		Dispatcher oldDispatcher = this.readEventDispatcher;
		this.readEventDispatcher = dispatcher;
		if (oldDispatcher != null) {
			oldDispatcher.stop();
		}
	}

	void setWriteEventDispatcher(Dispatcher dispatcher) {
		Dispatcher oldDispatcher = this.writeEventDispatcher;
		this.writeEventDispatcher = dispatcher;
		if (oldDispatcher != null) {
			oldDispatcher.stop();
		}
	}

	private final void startStatistics() {
		this.statistics.start();
	}

	public void notifyStarted() {
		for (ControllerStateListener stateListener : this.stateListeners) {
			stateListener.onStarted(this);
		}
	}

	public boolean isStarted() {
		return this.started;
	}

	public final Statistics getStatistics() {
		return this.statistics;
	}

	public final CodecFactory getCodecFactory() {
		return this.codecFactory;
	}

	public final void setCodecFactory(CodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	public void notifyReady() {
		for (ControllerStateListener stateListener : this.stateListeners) {
			stateListener.onReady(this);
		}
	}

	public final synchronized void unregisterSession(Session session) {
		this.sessionSet.remove(session);
		if (this.sessionSet.size() == 0) {
			notifyAllSessionClosed();
		}
	}

	public void checkStatisticsForRestart() {
		if (this.statisticsInterval > 0
				&& System.currentTimeMillis()
						- this.statistics.getStartedTime() > this.statisticsInterval * 1000) {
			this.statistics.restart();
		}
	}

	public final synchronized void registerSession(Session session) {
		if (this.started) {
			this.sessionSet.add(session);
		} else {
			session.close();
		}

	}

	public synchronized void stop() throws IOException {
		if (!isStarted()) {
			return;
		}
		setStarted(false);
		for (Session session : this.sessionSet) {
			session.close();
		}
		stopStatistics();
		stopDispatcher();
		this.sessionSet.clear();
		notifyStopped();
		clearStateListeners();
		stop0();
		log.info("Controller has been stopped.");

	}

	protected abstract void stop0() throws IOException;

	private final void stopDispatcher() {
		if (this.readEventDispatcher != null) {
			this.readEventDispatcher.stop();
		}
		if (this.dispatchMessageDispatcher != null) {
			this.dispatchMessageDispatcher.stop();
		}
		if (this.writeEventDispatcher != null) {
			this.writeEventDispatcher.stop();
		}
	}

	private final void stopStatistics() {
		this.statistics.stop();
	}

	private final void clearStateListeners() {
		this.stateListeners.clear();
	}

	public final void notifyException(Throwable t) {
		for (ControllerStateListener stateListener : this.stateListeners) {
			stateListener.onException(this, t);
		}
	}

	public final void notifyStopped() {
		for (ControllerStateListener stateListener : this.stateListeners) {
			stateListener.onStopped(this);
		}
	}

	public final void notifyAllSessionClosed() {
		for (ControllerStateListener stateListener : this.stateListeners) {
			stateListener.onAllSessionClosed(this);
		}
	}

	public Set<Session> getSessionSet() {
		return Collections.unmodifiableSet(this.sessionSet);
	}

	public <T> void setSocketOption(SocketOption<T> socketOption, T value) {
		if (socketOption == null) {
			throw new NullPointerException("Null socketOption");
		}
		if (value == null) {
			throw new NullPointerException("Null value");
		}
		if (!socketOption.type().equals(value.getClass())) {
			throw new IllegalArgumentException("Expected "
					+ socketOption.type().getSimpleName()
					+ " value,but givend " + value.getClass().getSimpleName());
		}
		this.socketOptions.put(socketOption, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getSocketOption(SocketOption<T> socketOption) {
		return (T) this.socketOptions.get(socketOption);
	}

	/**
	 * Bind localhost address
	 * 
	 * @param inetSocketAddress
	 * @throws IOException
	 */
	public void bind(InetSocketAddress inetSocketAddress) throws IOException {
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException("Null inetSocketAddress");
		}
		setLocalSocketAddress(inetSocketAddress);
		start();
	}
}
