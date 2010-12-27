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
package net.rubyeye.xmemcached.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedOptimizer;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.networking.Connector;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;
import net.rubyeye.xmemcached.utils.Protocol;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.Controller;
import com.google.code.yanf4j.core.ControllerStateListener;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.nio.impl.SocketChannelController;
import com.google.code.yanf4j.util.ConcurrentHashSet;
import com.google.code.yanf4j.util.SystemUtils;

/**
 * Connected session manager
 * 
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController implements
		Connector {

	private final DelayQueue<ReconnectRequest> waitingQueue = new DelayQueue<ReconnectRequest>();
	private BufferAllocator bufferAllocator;

	private final Set<InetSocketAddress> removedAddrSet = new ConcurrentHashSet<InetSocketAddress>();

	private final MemcachedOptimizer optimiezer;
	private volatile long healSessionInterval = 2000L;
	private int connectionPoolSize; // session pool size
	protected Protocol protocol;

	private final CommandFactory commandFactory;
	private volatile boolean failureMode;

	private final ConcurrentHashMap<InetSocketAddress/* Main node address */, List<Session>/*
																						 * standby
																						 * sessions
																						 */> standbySessionMap = new ConcurrentHashMap<InetSocketAddress, List<Session>>();

	public void setSessionLocator(MemcachedSessionLocator sessionLocator) {
		this.sessionLocator = sessionLocator;
	}

	/**
	 * Session monitor for healing sessions.
	 * 
	 * @author dennis
	 * 
	 */
	class SessionMonitor extends Thread {
		public SessionMonitor() {
			this.setName("Heal-Session-Thread");
		}

		@Override
		public void run() {
			while (MemcachedConnector.this.isStarted()) {

				try {
					ReconnectRequest request = MemcachedConnector.this.waitingQueue
							.take();

					InetSocketAddress address = request
							.getInetSocketAddressWrapper()
							.getInetSocketAddress();

					if (!MemcachedConnector.this.removedAddrSet
							.contains(address)) {
						boolean connected = false;
						Future<Boolean> future = MemcachedConnector.this
								.connect(request.getInetSocketAddressWrapper());
						request.setTries(request.getTries() + 1);
						try {
							log.warn("Trying to connect to "
									+ address.getAddress().getHostAddress()
									+ ":" + address.getPort() + " for "
									+ request.getTries() + " times");
							if (!future.get(
									MemcachedClient.DEFAULT_CONNECT_TIMEOUT,
									TimeUnit.MILLISECONDS)) {
								connected = false;
							} else {
								connected = true;
								break;
							}
						} catch (TimeoutException e) {
							future.cancel(true);
						} catch (ExecutionException e) {
							future.cancel(true);
						} finally {
							if (!connected) {
								// update timestamp for next reconnecting
								request
										.updateNextReconnectTimeStamp(MemcachedConnector.this.healSessionInterval
												* request.getTries());
								log.error("Reconnect to "
										+ address.getAddress().getHostAddress()
										+ ":" + address.getPort() + " fail");
								// add to tail
								MemcachedConnector.this.waitingQueue
										.offer(request);
							} else {
								continue;
							}
						}
					} else {
						log
								.warn("Remove invalid reconnect task for "
										+ address);
						// remove reconnect task
					}
				} catch (InterruptedException e) {
					// ignore,check status
				} catch (Exception e) {
					log.error("SessionMonitor connect error", e);
				}
			}
		}
	}

	public Queue<ReconnectRequest> getReconnectRequestQueue() {
		return this.waitingQueue;
	}

	@Override
	public Set<Session> getSessionSet() {
		Collection<Queue<Session>> sessionQueues = this.sessionMap.values();
		Set<Session> result = new HashSet<Session>();
		for (Queue<Session> queue : sessionQueues) {
			result.addAll(queue);
		}
		return result;
	}

	public final void setHealSessionInterval(long healConnectionInterval) {
		this.healSessionInterval = healConnectionInterval;
	}

	public long getHealSessionInterval() {
		return this.healSessionInterval;
	}

	public void setOptimizeGet(boolean optimiezeGet) {
		((OptimizerMBean) this.optimiezer).setOptimizeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		((OptimizerMBean) this.optimiezer)
				.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	public Protocol getProtocol() {
		return this.protocol;
	}

	protected MemcachedSessionLocator sessionLocator;

	protected final ConcurrentHashMap<InetSocketAddress, Queue<Session>> sessionMap = new ConcurrentHashMap<InetSocketAddress, Queue<Session>>();

	public synchronized void addSession(Session session) {
		MemcachedTCPSession tcpSession = (MemcachedTCPSession) session;
		InetSocketAddressWrapper addrWrapper = tcpSession
				.getInetSocketAddressWrapper();

		InetSocketAddress mainNodeAddress = addrWrapper.getMainNodeAddress();
		if (mainNodeAddress != null) {
			// It is a standby session
			this.addStandbySession(session, mainNodeAddress);
		} else {
			// It is a main session
			this.addMainSession(session);
			// Update main sessions
			this.updateSessions();
		}
	}

	private void addMainSession(Session session) {
		InetSocketAddress remoteSocketAddress = session
				.getRemoteSocketAddress();
		log.warn("Add a session: "
				+ SystemUtils.getRawAddress(remoteSocketAddress) + ":"
				+ remoteSocketAddress.getPort());
		Queue<Session> sessions = this.sessionMap.get(remoteSocketAddress);
		if (sessions == null) {
			sessions = new ConcurrentLinkedQueue<Session>();
			Queue<Session> oldSessions = this.sessionMap.putIfAbsent(
					remoteSocketAddress, sessions);
			if (null != oldSessions) {
				sessions = oldSessions;
			}
		}
		// If it is in failure mode,remove closed session from list
		if (this.failureMode) {
			Iterator<Session> it = sessions.iterator();
			while (it.hasNext()) {
				Session tmp = it.next();
				if (tmp.isClosed()) {
					it.remove();
					break;
				}
			}
		}

		sessions.offer(session);
		// Remove old session and close it
		while (sessions.size() > this.connectionPoolSize) {
			Session oldSession = sessions.poll();
			((MemcachedSession) oldSession).setAllowReconnect(false);
			oldSession.close();
		}
	}

	private void addStandbySession(Session session,
			InetSocketAddress mainNodeAddress) {
		InetSocketAddress remoteSocketAddress = session
				.getRemoteSocketAddress();
		log.warn("Add a standby session: "
				+ SystemUtils.getRawAddress(remoteSocketAddress) + ":"
				+ remoteSocketAddress.getPort() + " for "
				+ SystemUtils.getRawAddress(mainNodeAddress) + ":"
				+ mainNodeAddress.getPort());
		List<Session> sessions = this.standbySessionMap.get(mainNodeAddress);
		if (sessions == null) {
			sessions = new CopyOnWriteArrayList<Session>();
			List<Session> oldSessions = this.standbySessionMap.putIfAbsent(
					mainNodeAddress, sessions);
			if (null != oldSessions) {
				sessions = oldSessions;
			}
		}
		sessions.add(session);
	}

	public List<Session> getSessionListBySocketAddress(
			InetSocketAddress inetSocketAddress) {
		Queue<Session> queue = this.sessionMap.get(inetSocketAddress);
		if (queue != null) {
			return new ArrayList<Session>(queue);
		} else {
			return null;
		}
	}

	public void removeReconnectRequest(InetSocketAddress inetSocketAddress) {
		this.removedAddrSet.add(inetSocketAddress);
		Iterator<ReconnectRequest> it = this.waitingQueue.iterator();
		while (it.hasNext()) {
			ReconnectRequest request = it.next();
			if (request.getInetSocketAddressWrapper().getInetSocketAddress()
					.equals(inetSocketAddress)) {
				it.remove();
				log.warn("Remove invalid reconnect task for "
						+ request.getInetSocketAddressWrapper()
								.getInetSocketAddress());
			}
		}
	}

	private static final MemcachedSessionComparator sessionComparator = new MemcachedSessionComparator();

	public final void updateSessions() {
		Collection<Queue<Session>> sessionCollection = this.sessionMap.values();
		List<Session> sessionList = new ArrayList<Session>(20);
		for (Queue<Session> sessions : sessionCollection) {
			sessionList.addAll(sessions);
		}
		// sort the sessions to keep order
		Collections.sort(sessionList, sessionComparator);
		this.sessionLocator.updateSessions(sessionList);
	}

	public synchronized void removeSession(Session session) {
		MemcachedTCPSession tcpSession = (MemcachedTCPSession) session;
		InetSocketAddressWrapper addrWrapper = tcpSession
				.getInetSocketAddressWrapper();
		InetSocketAddress mainNodeAddr = addrWrapper.getMainNodeAddress();
		if (mainNodeAddr != null) {
			this.removeStandbySession(session, mainNodeAddr);
		} else {
			this.removeMainSession(session);
		}
	}

	private void removeMainSession(Session session) {
		// If it was in failure mode,we don't remove it from list.
		if (this.failureMode) {
			return;
		}
		InetSocketAddress remoteSocketAddress = session
				.getRemoteSocketAddress();
		log.warn("Remove a session: "
				+ SystemUtils.getRawAddress(remoteSocketAddress) + ":"
				+ remoteSocketAddress.getPort());
		Queue<Session> sessionQueue = this.sessionMap.get(session
				.getRemoteSocketAddress());
		if (null != sessionQueue) {
			sessionQueue.remove(session);
			if (sessionQueue.size() == 0) {
				this.sessionMap.remove(session.getRemoteSocketAddress());
			}
			this.updateSessions();
		}
	}

	private void removeStandbySession(Session session,
			InetSocketAddress mainNodeAddr) {
		List<Session> sessionList = this.standbySessionMap.get(mainNodeAddr);
		if (null != sessionList) {
			sessionList.remove(session);
			if (sessionList.size() == 0) {
				this.standbySessionMap.remove(mainNodeAddr);
			}
		}
	}

	@Override
	protected void doStart() throws IOException {
		this.setLocalSocketAddress(new InetSocketAddress("localhost", 0));
	}

	@Override
	public void onConnect(SelectionKey key) throws IOException {
		key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		ConnectFuture future = (ConnectFuture) key.attachment();
		if (future == null || future.isCancelled()) {
			key.channel().close();
			key.cancel();
			return;
		}
		try {
			if (!((SocketChannel) key.channel()).finishConnect()) {
				future.failure(new IOException("Connect to "
						+ SystemUtils.getRawAddress(future
								.getInetSocketAddressWrapper()
								.getInetSocketAddress())
						+ ":"
						+ future.getInetSocketAddressWrapper()
								.getInetSocketAddress().getPort() + " fail"));
			} else {
				key.attach(null);
				this.addSession(this.createSession((SocketChannel) key
						.channel(), future.getInetSocketAddressWrapper()));
				future.setResult(Boolean.TRUE);
			}
		} catch (Exception e) {
			future.failure(e);
			key.cancel();
			throw new IOException("Connect to "
					+ SystemUtils.getRawAddress(future
							.getInetSocketAddressWrapper()
							.getInetSocketAddress())
					+ ":"
					+ future.getInetSocketAddressWrapper()
							.getInetSocketAddress().getPort() + " fail,"
					+ e.getMessage());
		}
	}

	protected MemcachedTCPSession createSession(SocketChannel socketChannel,
			InetSocketAddressWrapper wrapper) {
		MemcachedTCPSession session = (MemcachedTCPSession) this
				.buildSession(socketChannel);
		session.setInetSocketAddressWrapper(wrapper);
		this.selectorManager.registerSession(session, EventType.ENABLE_READ);
		session.start();
		session.onEvent(EventType.CONNECTED, null);
		return session;
	}

	public void addToWatingQueue(ReconnectRequest request) {
		this.waitingQueue.add(request);
	}

	public Future<Boolean> connect(InetSocketAddressWrapper addressWrapper)
			throws IOException {
		if (addressWrapper == null) {
			throw new NullPointerException("Null Address");
		}
		// Remove addr from removed set
		this.removedAddrSet.remove(addressWrapper.getInetSocketAddress());
		SocketChannel socketChannel = SocketChannel.open();
		this.configureSocketChannel(socketChannel);
		ConnectFuture future = new ConnectFuture(addressWrapper);
		if (!socketChannel.connect(addressWrapper.getInetSocketAddress())) {
			this.selectorManager.registerChannel(socketChannel,
					SelectionKey.OP_CONNECT, future);
		} else {
			this.addSession(this.createSession(socketChannel, addressWrapper));
			future.setResult(true);
		}
		return future;
	}

	public void closeChannel(Selector selector) throws IOException {
		// do nothing
	}

	private final Random random = new Random();

	public void send(final Command msg) throws MemcachedException {
		MemcachedTCPSession session = (MemcachedTCPSession) this
				.findSessionByKey(msg.getKey());
		if (session == null) {
			throw new MemcachedException(
					"There is no available connection at this moment");
		}
		// If session was closed,try to use standby memcached node
		if (session.isClosed()) {
			session = this.findStandbySession(session);
		}
		if (session.isClosed()) {
			throw new MemcachedException(SystemUtils.getRawAddress(session
					.getRemoteSocketAddress())
					+ ":"
					+ session.getRemoteSocketAddress().getPort()
					+ " has been closed");
		}
		if (session.isAuthFailed()) {
			throw new MemcachedException("Auth failed to connection "
					+ session.getRemoteSocketAddress());
		}
		session.write(msg);
	}

	private MemcachedTCPSession findStandbySession(MemcachedTCPSession session) {
		if (this.failureMode) {
			List<Session> sessionList = this
					.getStandbySessionListByMainNodeAddr(session
							.getRemoteSocketAddress());
			if (sessionList != null && !sessionList.isEmpty()) {
				return (MemcachedTCPSession) sessionList.get(this.random
						.nextInt(sessionList.size()));
			}
		}
		return session;
	}

	/**
	 * Returns main node's standby session list.
	 * 
	 * @param addr
	 * @return
	 */
	public List<Session> getStandbySessionListByMainNodeAddr(
			InetSocketAddress addr) {
		return this.standbySessionMap.get(addr);
	}

	/**
	 * Inner state listenner,manage session monitor.
	 * 
	 * @author boyan
	 * 
	 */
	class InnerControllerStateListener implements ControllerStateListener {
		private final SessionMonitor sessionMonitor = new SessionMonitor();

		public void onAllSessionClosed(Controller controller) {

		}

		public void onException(Controller controller, Throwable t) {
			log.error("Exception occured in controller", t);
		}

		public void onReady(Controller controller) {
			this.sessionMonitor.start();
		}

		public void onStarted(Controller controller) {

		}

		public void onStopped(Controller controller) {
			this.sessionMonitor.interrupt();
		}

	}

	public final Session findSessionByKey(String key) {
		return this.sessionLocator.getSessionByKey(key);
	}

	/**
	 * Get session by InetSocketAddress
	 * 
	 * @param addr
	 * @return
	 */
	public final Queue<Session> getSessionByAddress(InetSocketAddress addr) {
		return this.sessionMap.get(addr);
	}

	public MemcachedConnector(Configuration configuration,
			MemcachedSessionLocator locator, BufferAllocator allocator,
			CommandFactory commandFactory, int poolSize) {
		super(configuration, null);
		this.sessionLocator = locator;
		this.protocol = commandFactory.getProtocol();
		this.addStateListener(new InnerControllerStateListener());
		this.updateSessions();
		this.bufferAllocator = allocator;
		this.optimiezer = new Optimizer(this.protocol);
		this.optimiezer.setBufferAllocator(this.bufferAllocator);
		this.connectionPoolSize = poolSize;
		this.soLingerOn = true;
		this.commandFactory = commandFactory;
		// setDispatchMessageThreadPoolSize(Runtime.getRuntime().
		// availableProcessors());
	}

	public final void setConnectionPoolSize(int poolSize) {
		this.connectionPoolSize = poolSize;
	}

	public void setMergeFactor(int mergeFactor) {
		((OptimizerMBean) this.optimiezer).setMergeFactor(mergeFactor);
	}

	@Override
	protected NioSession buildSession(SocketChannel sc) {
		Queue<WriteMessage> queue = this.buildQueue();
		final NioSessionConfig sessionCofig = this
				.buildSessionConfig(sc, queue);
		MemcachedTCPSession session = new MemcachedTCPSession(sessionCofig,
				this.configuration.getSessionReadBufferSize(), this.optimiezer,
				this.getReadThreadCount(), this.commandFactory);
		session.setBufferAllocator(this.bufferAllocator);
		return session;
	}

	public BufferAllocator getBufferAllocator() {
		return this.bufferAllocator;
	}

	public synchronized void quitAllSessions() {
		for (Session session : this.sessionSet) {
			((MemcachedSession) session).quit();
		}
		int sleepCount = 0;
		while (sleepCount++ < 5 && this.sessionSet.size() > 0) {
			try {
				this.wait(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	public void setFailureMode(boolean failureMode) {
		this.failureMode = failureMode;
	}

	public void setBufferAllocator(BufferAllocator allocator) {
		this.bufferAllocator = allocator;
		for (Session session : this.getSessionSet()) {
			((MemcachedSession) session).setBufferAllocator(allocator);
		}
	}

	public Collection<InetSocketAddress> getServerAddresses() {
		return Collections.unmodifiableCollection(this.sessionMap.keySet());
	}
}
