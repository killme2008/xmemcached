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
import java.util.List;
import java.util.Queue;
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
import net.rubyeye.xmemcached.utils.Protocol;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.nio.impl.SocketChannelController;

/**
 * Connected session manager
 * 
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController {

	private final BlockingQueue<ReconnectRequest> waitingQueue = new LinkedBlockingQueue<ReconnectRequest>();
	private BufferAllocator bufferAllocator;
	private final SessionMonitor sessionMonitor;
	private final MemcachedOptimizer optimiezer;
	private volatile long healSessionInterval = 2000L;
	private int connectionPoolSize; // session pool size

	class SessionMonitor extends Thread {

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				try {
					ReconnectRequest request = MemcachedConnector.this.waitingQueue
							.take();
					InetSocketAddress address = request.getAddress();
					boolean connected = false;
					int tries = 0;
					while (tries < 1) {
						Future<Boolean> future = connect(address, request
								.getWeight());
						tries++;
						request.setTries(request.getTries() + 1);
						try {
							log.warn("Try to reconnect to "
									+ address.getHostName() + ":"
									+ address.getPort() + " for "
									+ request.getTries() + " times");
							if (!future.get(
									MemcachedClient.DEFAULT_CONNECT_TIMEOUT,
									TimeUnit.MILLISECONDS)) {
								Thread
										.sleep(MemcachedConnector.this.healSessionInterval);
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
							Thread
									.sleep(MemcachedConnector.this.healSessionInterval);
							continue;
						}
					}
					if (!connected) {
						log.error("reconnect to " + address.getHostName() + ":"
								+ address.getPort() + " fail");
						// add to tail
						MemcachedConnector.this.waitingQueue.add(request);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					log.error("SessionMonitor connect error", e);
				}
			}
		}
	}

	public final void setHealSessionInterval(long healConnectionInterval) {
		this.healSessionInterval = healConnectionInterval;
	}

	public void setOptimizeGet(boolean optimiezeGet) {
		((OptimizerMBean) this.optimiezer).setOptimizeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		((OptimizerMBean) this.optimiezer)
				.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	protected MemcachedSessionLocator sessionLocator;

	private final ConcurrentHashMap<InetSocketAddress, Queue<Session>> sessionMap = new ConcurrentHashMap<InetSocketAddress, Queue<Session>>();

	public void addSession(MemcachedTCPSession session) {
		log.warn("add session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		Queue<Session> sessions = this.sessionMap.get(session
				.getRemoteSocketAddress());
		if (sessions == null) {
			sessions = new ConcurrentLinkedQueue<Session>();
			Queue<Session> oldSessions = this.sessionMap.putIfAbsent(session
					.getRemoteSocketAddress(), sessions);
			if (null != oldSessions) {
				sessions = oldSessions;
			}
		}
		sessions.offer(session);
		// Remove old session and close it
		while (sessions.size() > this.connectionPoolSize) {
			Session oldSession = sessions.poll();
			((MemcachedTCPSession) oldSession).setAllowReconnect(false);
			oldSession.close();
		}
		updateSessions();
	}

	public List<Session> getSessionListBySocketAddress(
			InetSocketAddress inetSocketAddress) {
		if (this.sessionMap.get(inetSocketAddress) != null) {
			return new ArrayList<Session>(this.sessionMap
					.get(inetSocketAddress));
		} else {
			return null;
		}
	}

	public final void updateSessions() {
		Collection<Queue<Session>> sessionCollection = this.sessionMap.values();
		List<Session> sessionList = new ArrayList<Session>(20);
		for (Queue<Session> sessions : sessionCollection) {
			sessionList.addAll(sessions);
		}
		this.sessionLocator.updateSessions(sessionList);
	}

	public void removeSession(MemcachedTCPSession session) {
		log.warn("remove session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		Queue<Session> sessionQueue = this.sessionMap.get(session
				.getRemoteSocketAddress());
		if (null != sessionQueue) {
			sessionQueue.remove(session);
			if (sessionQueue.size() == 0) {
				this.sessionMap.remove(session.getRemoteSocketAddress());
			}
		}
		updateSessions();
	}

	@Override
	protected void doStart() throws IOException {
		this.sessionMonitor.start();
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
				future.setException(new IOException("Connect to "
						+ future.getInetSocketAddress().getHostName() + ":"
						+ future.getInetSocketAddress().getPort() + " fail"));
			} else {
				key.attach(null);
				addSession(createSession((SocketChannel) key.channel(), future
						.getWeight()));
				future.setConnected(true);
			}
		} catch (Exception e) {
			future.setException(e);
			throw new IOException("Connect to "
					+ future.getInetSocketAddress().getHostName() + ":"
					+ future.getInetSocketAddress().getPort() + " fail,"
					+ e.getMessage());
		}
	}

	protected MemcachedTCPSession createSession(SocketChannel socketChannel,
			int weight) {
		MemcachedTCPSession session = (MemcachedTCPSession) buildSession(socketChannel);
		session.setWeight(weight);
		this.selectorManager.registerSession(session, EventType.ENABLE_READ);
		session.start();
		session.onEvent(EventType.CONNECTED, null);
		return session;
	}

	public void addToWatingQueue(ReconnectRequest request) {
		this.waitingQueue.add(request);
	}

	public Future<Boolean> connect(InetSocketAddress address, int weight)
			throws IOException {
		if (address == null) {
			throw new NullPointerException("Null Address");
		}
		SocketChannel socketChannel = SocketChannel.open();
		configureSocketChannel(socketChannel);
		ConnectFuture future = new ConnectFuture(address, weight);
		if (!socketChannel.connect(address)) {
			this.selectorManager.registerChannel(socketChannel,
					SelectionKey.OP_CONNECT, future);
		} else {
			addSession(createSession(socketChannel, weight));
			future.setConnected(true);
		}
		return future;
	}

	public void closeChannel(Selector selector) throws IOException {
		this.sessionMonitor.interrupt();
		while (this.sessionMonitor.isAlive()) {
			try {
				this.sessionMonitor.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public final void send(final Command msg) throws MemcachedException {
		Session session = findSessionByKey(msg.getKey());
		if (session == null) {
			throw new MemcachedException(
					"There is no avriable session at this moment");
		}
		session.write(msg);
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
			Protocol protocol, int poolSize) {
		super(configuration, null);
		this.sessionLocator = locator;
		updateSessions();
		this.sessionMonitor = new SessionMonitor();
		this.bufferAllocator = allocator;
		this.optimiezer = new Optimizer(protocol);
		this.optimiezer.setBufferAllocator(this.bufferAllocator);
		this.connectionPoolSize = poolSize;
		this.soLingerOn = true;
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
		Queue<WriteMessage> queue = buildQueue();
		final NioSessionConfig sessionCofig = buildSessionConfig(sc, queue);
		MemcachedTCPSession session = new MemcachedTCPSession(sessionCofig,
				this.configuration.getSessionReadBufferSize(), this.optimiezer,
				this.getReadThreadCount());
		session.setBufferAllocator(this.bufferAllocator);
		return session;
	}

	public BufferAllocator getBufferAllocator() {
		return this.bufferAllocator;
	}

	public void setBufferAllocator(BufferAllocator allocator) {
		this.bufferAllocator = allocator;
		for (Session session : getSessionSet()) {
			((MemcachedTCPSession) session).setBufferAllocator(allocator);
		}
	}

	public Collection<InetSocketAddress> getServerAddresses() {
		return Collections.unmodifiableCollection(this.sessionMap.keySet());
	}
}
