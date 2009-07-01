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
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedOptimizer;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.WriteMessage;
import com.google.code.yanf4j.nio.impl.SessionConfig;
import com.google.code.yanf4j.nio.impl.SocketChannelController;
import com.google.code.yanf4j.nio.util.EventType;

/**
 * Connected session manager
 * 
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController {

	private final BlockingQueue<ReconnectRequest> waitingQueue = new LinkedBlockingQueue<ReconnectRequest>();
	private BufferAllocator bufferAllocator;
	private SessionMonitor sessionMonitor;
	private MemcachedOptimizer optimiezer;

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
					while (tries < 3) {
						Future<Boolean> future = connect(address, request
								.getWeight());
						tries++;
						request.setTries(request.getTries() + 1);
						try {
							log.warn("Try to reconnect to "
									+ address.getHostName() + ":"
									+ address.getPort() + " for "
									+ request.getTries() + " times");
							if (!future.isDone()
									&& !future
											.get(
													MemcachedClient.DEFAULT_CONNECT_TIMEOUT,
													TimeUnit.MILLISECONDS)) {
								Thread.sleep(2000);
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

	public void setOptimizeGet(boolean optimiezeGet) {
		((OptimizerMBean) this.optimiezer).setOptimizeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		((OptimizerMBean) this.optimiezer)
				.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	protected MemcachedSessionLocator sessionLocator;

	static class ConnectFuture implements Future<Boolean> {

		private int weight;
		private boolean connected = false;
		private boolean done = false;
		private boolean cancel = false;
		private Lock lock = new ReentrantLock();
		private Condition notDone = this.lock.newCondition();
		private volatile Exception exception;
		private InetSocketAddress inetSocketAddress;

		public ConnectFuture(InetSocketAddress inetSocketAddress, int weight) {
			super();
			this.inetSocketAddress = inetSocketAddress;
			this.weight = weight;
		}

		public final InetSocketAddress getInetSocketAddress() {
			return this.inetSocketAddress;
		}

		public final int getWeight() {
			return this.weight;
		}

		public final void setWeight(int weight) {
			this.weight = weight;
		}

		public boolean isConnected() {
			this.lock.lock();
			try {
				return this.connected;
			} finally {
				this.lock.unlock();
			}
		}

		public void setConnected(boolean connected) {
			this.lock.lock();
			try {
				this.connected = connected;
				this.done = true;
				this.notDone.signalAll();
			} finally {
				this.lock.unlock();
			}
		}

		public Exception getException() {
			this.lock.lock();
			try {
				return this.exception;
			} finally {
				this.lock.unlock();
			}
		}

		public void setException(Exception exception) {
			this.lock.lock();
			try {
				this.exception = exception;
				this.done = true;
				this.notDone.signalAll();
			} finally {
				this.lock.unlock();
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			this.lock.lock();
			try {
				this.cancel = true;
				return this.cancel;
			} finally {
				this.lock.unlock();
			}
		}

		@Override
		public Boolean get() throws InterruptedException, ExecutionException {
			this.lock.lock();
			try {
				while (!this.done) {
					this.notDone.await();
				}
				if (this.exception != null) {
					throw new ExecutionException(this.exception);
				}
				return this.connected;
			} finally {
				this.lock.unlock();
			}
		}

		@Override
		public Boolean get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			this.lock.lock();
			try {

				while (!this.done) {
					if (!this.notDone.await(timeout, unit)) {
						throw new TimeoutException("connect timeout");
					}
				}
				if (this.exception != null) {
					throw new ExecutionException(this.exception);
				}
				return this.connected;
			} finally {
				this.lock.unlock();
			}
		}

		@Override
		public boolean isCancelled() {
			this.lock.lock();
			try {
				return this.cancel;
			} finally {
				this.lock.unlock();
			}
		}

		@Override
		public boolean isDone() {
			this.lock.lock();
			try {
				return this.done;
			} finally {
				this.lock.unlock();
			}
		}
	}

	private final ConcurrentHashMap<InetSocketAddress, Session> sessionMap = new ConcurrentHashMap<InetSocketAddress, Session>();

	public void addSession(MemcachedTCPSession session) {
		log.warn("add session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		Session oldSession = this.sessionMap.put(session
				.getRemoteSocketAddress(), session);
		if (oldSession != null) {
			oldSession.close();
		}
		updateSessions();
	}

	public final void updateSessions() {
		this.sessionLocator.updateSessions(this.sessionMap.values());
	}

	public void removeSession(MemcachedTCPSession session) {
		log.warn("remove session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		this.sessionMap.remove(session.getRemoteSocketAddress());
		updateSessions();
	}

	private int sendBufferSize = 0;
	private MemcachedTCPSession session;

	public final int getSendBufferSize() {
		return this.sendBufferSize;
	}

	public final void setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}

	@Override
	protected void doStart() throws IOException {
		this.sessionMonitor.start();
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
			if (!((SocketChannel) (key.channel())).finishConnect()) {
				future.setException(new IOException("Connect to "
						+ future.getInetSocketAddress().getHostName() + ":"
						+ future.getInetSocketAddress().getPort() + " fail"));
			} else {
				key.attach(null);
				addSession(createSession(key, (SocketChannel) (key.channel()),
						future.getWeight()));
				future.setConnected(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			future.setException(e);
			throw new IOException("Connect to "
					+ future.getInetSocketAddress().getHostName() + ":"
					+ future.getInetSocketAddress().getPort() + " fail", e);
		}
	}

	protected MemcachedTCPSession createSession(SelectionKey key,
			SocketChannel socketChannel, int weight) {
		key.attach(weight);
		MemcachedTCPSession session = (MemcachedTCPSession) buildSession(
				socketChannel, key);
		session.onEvent(EventType.ENABLE_READ, this.selector);
		key.attach(session);
		session.start();
		session.onEvent(EventType.CONNECTED, this.selector);
		return session;
	}

	public void addToWatingQueue(ReconnectRequest request) {
		this.waitingQueue.add(request);
	}

	public Future<Boolean> connect(InetSocketAddress address, int weight)
			throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setSoTimeout(this.soTimeout);
		socketChannel.socket().setReuseAddress(this.reuseAddress);

		if (this.receiveBufferSize > 0) {
			socketChannel.socket().setReceiveBufferSize(this.receiveBufferSize);

		}
		socketChannel.socket().bind(this.socketAddress);
		if (this.sendBufferSize > 0) {
			socketChannel.socket().setSendBufferSize(this.sendBufferSize);
		}
		ConnectFuture future = new ConnectFuture(address, weight);
		if (!socketChannel.connect(address)) {
			this.reactor.registerChannel(socketChannel,
					SelectionKey.OP_CONNECT, future);
			this.reactor.wakeup();
		} else {
			SelectionKey selectionKey = socketChannel
					.register(this.selector, 0);
			addSession(createSession(selectionKey, socketChannel, weight));
			future.setConnected(true);
		}
		return future;
	}

	public void closeChannel() throws IOException {
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

		session.send(msg);
	}

	protected final Session findSessionByKey(String key) {
		return this.sessionLocator.getSessionByKey(key);
	}

	/**
	 * Get session by InetSocketAddress
	 * 
	 * @param addr
	 * @return
	 */
	public final Session getSessionByAddress(InetSocketAddress addr) {
		return this.sessionMap.get(addr);
	}

	public MemcachedConnector(Configuration configuration,
			MemcachedSessionLocator locator, BufferAllocator allocator) {
		super(configuration, null);
		this.sessionLocator = locator;
		updateSessions();
		this.sessionMonitor = new SessionMonitor();
		this.bufferAllocator = allocator;
		this.optimiezer = new Optimizer();
		this.optimiezer.setBufferAllocator(this.bufferAllocator);
		// setDispatchMessageThreadPoolSize(Runtime.getRuntime().availableProcessors());
	}

	public void setMergeFactor(int mergeFactor) {
		((OptimizerMBean) this.optimiezer).setMergeFactor(mergeFactor);
	}

	@Override
	protected Session buildSession(SocketChannel sc, SelectionKey selectionKey) {
		Queue<WriteMessage> queue = buildQueue();
		final SessionConfig sessionCofig = buildSessionConfig(sc, selectionKey,
				queue);
		int weight = selectionKey.attachment() == null ? 1
				: (Integer) selectionKey.attachment();
		this.session = new MemcachedTCPSession(sessionCofig, this.configuration
				.getSessionReadBufferSize(), this.optimiezer, this
				.getReadThreadCount(), weight);
		this.session.setBufferAllocator(this.bufferAllocator);
		return this.session;
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
