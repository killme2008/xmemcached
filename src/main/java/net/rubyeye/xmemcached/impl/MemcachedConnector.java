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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

/**
 * Connected session manager
 * 
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController implements
		Connector {

	private final BlockingQueue<ReconnectRequest> waitingQueue = new LinkedBlockingQueue<ReconnectRequest>();
	private BufferAllocator bufferAllocator;

	private final Set<InetSocketAddress> removedAddrSet = new ConcurrentHashSet<InetSocketAddress>();

	private final MemcachedOptimizer optimiezer;
	private volatile long healSessionInterval = 2000L;
	private int connectionPoolSize; // session pool size
	protected Protocol protocol;

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

		@Override
		public void run() {
			while (isStarted()) {

				try {
					ReconnectRequest request = waitingQueue.take();
					InetSocketAddress address = request
							.getInetSocketAddressWrapper()
							.getInetSocketAddress();

					if (!removedAddrSet.contains(address)) {
						boolean connected = false;
						int tries = 0;
						while (tries < 1) {
							Future<Boolean> future = connect(request
									.getInetSocketAddressWrapper(), request
									.getWeight());
							tries++;
							request.setTries(request.getTries() + 1);
							try {
								log.warn("Trying to connect to "
										+ address.getHostName() + ":"
										+ address.getPort() + " for "
										+ request.getTries() + " times");
								if (!future
										.get(
												MemcachedClient.DEFAULT_CONNECT_TIMEOUT,
												TimeUnit.MILLISECONDS)) {
									Thread.sleep(healSessionInterval);
									continue;
								} else {
									connected = true;
									break;
								}
							} catch (TimeoutException e) {
								future.cancel(true);
								continue;
							} catch (ExecutionException e) {
								future.cancel(true);
								Thread.sleep(healSessionInterval);
								continue;
							}
						}
						if (!connected) {
							log.error("Reconnect to " + address.getHostName()
									+ ":" + address.getPort() + " fail");
							// add to tail
							waitingQueue.add(request);
						}
					} else {
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

	@Override
	public Set<Session> getSessionSet() {
		Collection<Queue<Session>> sessionQueues = sessionMap.values();
		Set<Session> result = new HashSet<Session>();
		for (Queue<Session> queue : sessionQueues) {
			result.addAll(queue);
		}
		return result;
	}

	public final void setHealSessionInterval(long healConnectionInterval) {
		healSessionInterval = healConnectionInterval;
	}

	public void setOptimizeGet(boolean optimiezeGet) {
		((OptimizerMBean) optimiezer).setOptimizeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		((OptimizerMBean) optimiezer)
				.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	public Protocol getProtocol() {
		return protocol;
	}

	protected MemcachedSessionLocator sessionLocator;

	protected final ConcurrentHashMap<InetSocketAddress, Queue<Session>> sessionMap = new ConcurrentHashMap<InetSocketAddress, Queue<Session>>();

	public void addSession(Session session) {
		log.warn("add session: " + session.getRemoteSocketAddress());
		Queue<Session> sessions = sessionMap.get(session
				.getRemoteSocketAddress());
		if (sessions == null) {
			sessions = new ConcurrentLinkedQueue<Session>();
			Queue<Session> oldSessions = sessionMap.putIfAbsent(session
					.getRemoteSocketAddress(), sessions);
			if (null != oldSessions) {
				sessions = oldSessions;
			}
		}
		sessions.offer(session);
		// Remove old session and close it
		while (sessions.size() > connectionPoolSize) {
			Session oldSession = sessions.poll();
			((MemcachedSession) oldSession).setAllowReconnect(false);
			oldSession.close();
		}
		updateSessions();
	}

	public List<Session> getSessionListBySocketAddress(
			InetSocketAddress inetSocketAddress) {
		Queue<Session> queue = sessionMap.get(inetSocketAddress);
		if (queue != null) {
			return new ArrayList<Session>(queue);
		} else {
			return null;
		}
	}

	public void removeReconnectRequest(InetSocketAddress inetSocketAddress) {
		removedAddrSet.add(inetSocketAddress);
		Iterator<ReconnectRequest> it = waitingQueue.iterator();
		while (it.hasNext()) {
			ReconnectRequest request = it.next();
			if (request.getInetSocketAddressWrapper().getInetSocketAddress()
					.equals(inetSocketAddress)) {
				it.remove();
			}
		}
	}

	private static final MemcachedSessionComparator sessionComparator = new MemcachedSessionComparator();

	public final void updateSessions() {
		Collection<Queue<Session>> sessionCollection = sessionMap.values();
		List<Session> sessionList = new ArrayList<Session>(20);
		for (Queue<Session> sessions : sessionCollection) {
			sessionList.addAll(sessions);
		}
		// sort the sessions to keep order
		Collections.sort(sessionList, sessionComparator);
		sessionLocator.updateSessions(sessionList);
	}

	public void removeSession(Session session) {
		log.warn("remove session " + session.getRemoteSocketAddress());
		Queue<Session> sessionQueue = sessionMap.get(session
				.getRemoteSocketAddress());
		if (null != sessionQueue) {
			sessionQueue.remove(session);
			if (sessionQueue.size() == 0) {
				sessionMap.remove(session.getRemoteSocketAddress());
			}
			updateSessions();
		}
	}

	@Override
	protected void doStart() throws IOException {
		setLocalSocketAddress(new InetSocketAddress("localhost", 0));
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
						+ future.getInetSocketAddress().getHostName() + ":"
						+ future.getInetSocketAddress().getPort() + " fail"));
			} else {
				key.attach(null);
				addSession(createSession((SocketChannel) key.channel(), future
						.getWeight(), future.getOrder()));
				future.setResult(Boolean.TRUE);
			}
		} catch (Exception e) {
			future.failure(e);
			key.cancel();
			throw new IOException("Connect to "
					+ future.getInetSocketAddress().getHostName() + ":"
					+ future.getInetSocketAddress().getPort() + " fail,"
					+ e.getMessage());
		}
	}

	protected MemcachedTCPSession createSession(SocketChannel socketChannel,
			int weight, int order) {
		MemcachedTCPSession session = (MemcachedTCPSession) buildSession(socketChannel);
		session.setWeight(weight);
		session.setOrder(order);
		selectorManager.registerSession(session, EventType.ENABLE_READ);
		session.start();
		session.onEvent(EventType.CONNECTED, null);
		return session;
	}

	public void addToWatingQueue(ReconnectRequest request) {
		waitingQueue.add(request);
	}

	public Future<Boolean> connect(InetSocketAddressWrapper addressWrapper,
			int weight) throws IOException {
		if (addressWrapper == null) {
			throw new NullPointerException("Null Address");
		}
		removedAddrSet.remove(addressWrapper.getInetSocketAddress());
		SocketChannel socketChannel = SocketChannel.open();
		configureSocketChannel(socketChannel);
		ConnectFuture future = new ConnectFuture(addressWrapper, weight);
		if (!socketChannel.connect(addressWrapper.getInetSocketAddress())) {
			selectorManager.registerChannel(socketChannel,
					SelectionKey.OP_CONNECT, future);
		} else {
			addSession(createSession(socketChannel, weight, addressWrapper
					.getOrder()));
			future.setResult(true);
		}
		return future;
	}

	public void closeChannel(Selector selector) throws IOException {
		// do nothing
	}

	public void send(final Command msg) throws MemcachedException {
		MemcachedTCPSession session = (MemcachedTCPSession) findSessionByKey(msg
				.getKey());
		if (session == null || session.isClosed()) {
			throw new MemcachedException(
					"There is no available connection at this moment");
		}
		if (session.isAuthFailed())
			throw new MemcachedException("Auth failed to connection "
					+ session.getRemoteSocketAddress());
		session.write(msg);
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
			sessionMonitor.start();
		}

		public void onStarted(Controller controller) {

		}

		public void onStopped(Controller controller) {
			sessionMonitor.interrupt();
		}

	}

	public final Session findSessionByKey(String key) {
		return sessionLocator.getSessionByKey(key);
	}

	/**
	 * Get session by InetSocketAddress
	 * 
	 * @param addr
	 * @return
	 */
	public final Queue<Session> getSessionByAddress(InetSocketAddress addr) {
		return sessionMap.get(addr);
	}

	public MemcachedConnector(Configuration configuration,
			MemcachedSessionLocator locator, BufferAllocator allocator,
			Protocol protocol, int poolSize) {
		super(configuration, null);
		sessionLocator = locator;
		addStateListener(new InnerControllerStateListener());
		updateSessions();
		bufferAllocator = allocator;
		optimiezer = new Optimizer(protocol);
		optimiezer.setBufferAllocator(bufferAllocator);
		connectionPoolSize = poolSize;
		this.protocol = protocol;
		soLingerOn = true;
		// setDispatchMessageThreadPoolSize(Runtime.getRuntime().
		// availableProcessors());
	}

	public final void setConnectionPoolSize(int poolSize) {
		connectionPoolSize = poolSize;
	}

	public void setMergeFactor(int mergeFactor) {
		((OptimizerMBean) optimiezer).setMergeFactor(mergeFactor);
	}

	@Override
	protected NioSession buildSession(SocketChannel sc) {
		Queue<WriteMessage> queue = buildQueue();
		final NioSessionConfig sessionCofig = buildSessionConfig(sc, queue);
		MemcachedTCPSession session = new MemcachedTCPSession(sessionCofig,
				configuration.getSessionReadBufferSize(), optimiezer,
				getReadThreadCount());
		session.setBufferAllocator(bufferAllocator);
		return session;
	}

	public BufferAllocator getBufferAllocator() {
		return bufferAllocator;
	}

	public void setBufferAllocator(BufferAllocator allocator) {
		bufferAllocator = allocator;
		for (Session session : getSessionSet()) {
			((MemcachedSession) session).setBufferAllocator(allocator);
		}
	}

	public Collection<InetSocketAddress> getServerAddresses() {
		return Collections.unmodifiableCollection(sessionMap.keySet());
	}
}
