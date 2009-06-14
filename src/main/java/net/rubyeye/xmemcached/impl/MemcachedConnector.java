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

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.WriteMessage;
import com.google.code.yanf4j.nio.impl.SessionConfig;
import com.google.code.yanf4j.nio.impl.SocketChannelController;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.util.Queue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
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
import net.rubyeye.xmemcached.MemcachedOptimiezer;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.SimpleDeque;

/**
 * Connected session manager
 * 
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController {

	private final BlockingQueue<ReconnectRequest> waitingQueue = new LinkedBlockingQueue<ReconnectRequest>();
	private BufferAllocator bufferAllocator;
	private SessionMonitor sessionMonitor;
	private MemcachedOptimiezer optimiezer;

	public static final long CHECK_RECONNECT_INTERVAL = 10000L;

	class SessionMonitor extends Thread {

		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				try {
					ReconnectRequest request = waitingQueue.take();
					InetSocketAddress address = request.getAddress();
					boolean connected = false;
					int tries = 0;
					while (tries < 3) {
						Future<Boolean> future = connect(address, request
								.getWeight());
						tries++;
						request.setTries(request.getTries() + 1);
						try {
							log.warn("try to reconnect to "
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
						waitingQueue.add(request);
						Thread.sleep(CHECK_RECONNECT_INTERVAL);
					}
				} catch (IOException e) {
					log.error("monitor connect error", e);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public void setOptimiezeGet(boolean optimiezeGet) {
		((OptimiezerMBean) this.optimiezer).setOptimiezeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		((OptimiezerMBean) this.optimiezer)
				.setOptimiezeMergeBuffer(optimizeMergeBuffer);
	}

	protected MemcachedSessionLocator sessionLocator;

	static class ConnectFuture implements Future<Boolean> {

		private int weight;
		private boolean connected = false;
		private boolean done = false;
		private boolean cancel = false;
		private Lock lock = new ReentrantLock();
		private Condition notDone = lock.newCondition();
		private volatile Exception exception;
		private InetSocketAddress inetSocketAddress;

		public ConnectFuture(InetSocketAddress inetSocketAddress, int weight) {
			super();
			this.inetSocketAddress = inetSocketAddress;
			this.weight = weight;
		}

		public final InetSocketAddress getInetSocketAddress() {
			return inetSocketAddress;
		}

		public final int getWeight() {
			return weight;
		}

		public final void setWeight(int weight) {
			this.weight = weight;
		}

		public boolean isConnected() {
			lock.lock();
			try {
				return connected;
			} finally {
				lock.unlock();
			}
		}

		public void setConnected(boolean connected) {
			lock.lock();
			try {
				this.connected = connected;
				done = true;
				notDone.signalAll();
			} finally {
				lock.unlock();
			}
		}

		public Exception getException() {
			lock.lock();
			try {
				return this.exception;
			} finally {
				lock.unlock();
			}
		}

		public void setException(Exception exception) {
			lock.lock();
			try {
				this.exception = exception;
				done = true;
				notDone.signalAll();
			} finally {
				lock.unlock();
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			lock.lock();
			try {
				cancel = true;
				return cancel;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public Boolean get() throws InterruptedException, ExecutionException {
			lock.lock();
			try {
				while (!done)
					notDone.await();
				if (this.exception != null) {
					throw new ExecutionException(exception);
				}
				return connected;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public Boolean get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			lock.lock();
			try {

				while (!done) {
					if (!notDone.await(timeout, unit))
						throw new TimeoutException("connect timeout");
				}
				if (this.exception != null) {
					throw new ExecutionException(exception);
				}
				return connected;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public boolean isCancelled() {
			lock.lock();
			try {
				return cancel;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public boolean isDone() {
			lock.lock();
			try {
				return done;
			} finally {
				lock.unlock();
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
		if (oldSession != null)
			oldSession.close();
		this.sessionLocator.updateSessions(sessionMap.values());
	}

	public void removeSession(MemcachedTCPSession session) {
		log.warn("remove session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		this.sessionMap.remove(session.getRemoteSocketAddress());
		this.sessionLocator.updateSessions(sessionMap.values());
	}

	private int sendBufferSize = 0;
	private MemcachedTCPSession session;

	public final int getSendBufferSize() {
		return sendBufferSize;
	}

	public final void setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}

	@Override
	protected void doStart() throws IOException {
		this.sessionMonitor.start();
	}

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
		session.onEvent(EventType.ENABLE_READ, selector);
		key.attach(session);
		session.start();
		session.onEvent(EventType.CONNECTED, selector);
		return session;
	}

	public void addToWatingQueue(ReconnectRequest request) {
		this.waitingQueue.add(request);
	}

	public Future<Boolean> connect(InetSocketAddress address, int weight)
			throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setSoTimeout(timeout);
		socketChannel.socket().setReuseAddress(reuseAddress);

		if (this.receiveBufferSize > 0) {
			socketChannel.socket().setReceiveBufferSize(receiveBufferSize);

		}
		socketChannel.socket().bind(this.socketAddress);
		if (this.sendBufferSize > 0) {
			socketChannel.socket().setSendBufferSize(this.sendBufferSize);
		}
		ConnectFuture future = new ConnectFuture(address,weight);
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
		while (sessionMonitor.isAlive()) {
			try {
				this.sessionMonitor.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public final boolean send(final Command msg) throws MemcachedException {
		Session session = findSessionByKey((String) msg.getKey());
		if (session == null) {
			throw new MemcachedException(
					"There is no avriable session at this moment");
		}
		return session.send(msg);
	}

	protected final Session findSessionByKey(String key) {
		return sessionLocator.getSessionByKey(key);
	}

	/**
	 * get session through InetSocketAddress
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
		this.sessionLocator.updateSessions(this.sessionMap.values());
		this.sessionMonitor = new SessionMonitor();
		this.bufferAllocator = allocator;
		this.optimiezer = new Optimiezer();
		this.optimiezer.setBufferAllocator(this.bufferAllocator);
	}

	/**
	 * use dequeue
	 */
	protected Queue<WriteMessage> buildQueue() {
		return new SimpleDeque<WriteMessage>(500);
	}

	public void setMergeFactor(int mergeFactor) {
		((OptimiezerMBean) this.optimiezer).setMergeFactor(mergeFactor);
	}

	protected Session buildSession(SocketChannel sc, SelectionKey selectionKey) {
		Queue<WriteMessage> queue = buildQueue();
		final SessionConfig sessionCofig = buildSessionConfig(sc, selectionKey,
				queue);
		int weight = selectionKey.attachment() == null ? 1
				: (Integer) selectionKey.attachment();
		session = new MemcachedTCPSession(sessionCofig, configuration
				.getSessionReadBufferSize(), this.optimiezer, this
				.getReadThreadCount(), weight);
		session.setBufferAllocator(bufferAllocator);
		return session;
	}

	public BufferAllocator getBufferAllocator() {
		return bufferAllocator;
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
