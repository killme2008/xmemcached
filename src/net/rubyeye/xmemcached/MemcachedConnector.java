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
package net.rubyeye.xmemcached;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.SessionConfig;
import com.google.code.yanf4j.nio.impl.SocketChannelController;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.util.Queue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.OptimiezerImpl;
import net.rubyeye.xmemcached.utils.SimpleDeque;

/**
 * 针对memcached的连接管理类
 *
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController {

	public static class ReconnectRequest {

		InetSocketAddress address;
		int tries;

		public ReconnectRequest(InetSocketAddress address, int tries) {
			super();
			this.address = address;
			this.tries = tries; // 记录重连次数
		}
	}

	private final BlockingQueue<ReconnectRequest> waitingQueue = new LinkedBlockingQueue<ReconnectRequest>();
	private BufferAllocator bufferAllocator;
	private SessionMonitor sessionMonitor;
	private Optimiezer optimiezer;

	class SessionMonitor extends Thread {

		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				try {
					ReconnectRequest request = waitingQueue.take();
					InetSocketAddress address = request.address;
					boolean connected = false;
					int tries = 0;
					while (tries < 3) {
						Future<Boolean> future = connect(address);
						tries++;
						request.tries++;
						try {
							log.warn("try to connect to "
									+ address.getHostName() + ":"
									+ address.getPort() + " for "
									+ request.tries + " times");
							if (!future.isDone()
									&& !future
											.get(
													XMemcachedClient.DEFAULT_CONNECT_TIMEOUT,
													TimeUnit.MILLISECONDS)) {
								Thread.sleep(2000); // 2秒后再次重连
								continue;
							} else {
								connected = true;
								break;
							}
						} catch (TimeoutException e) {
							future.cancel(true);
							Thread.sleep(2000); // 2秒后再次重连
							continue;
						} catch (ExecutionException e) {
							future.cancel(true);
							Thread.sleep(2000); // 2秒后再次重连
							continue;
						}
					}
					if (!connected) {
						log.error("connect to " + address.getHostName() + ":"
								+ address.getPort() + " fail");
						// 加入队尾,稍后重试
						waitingQueue.add(request);
						Thread.sleep(XMemcachedClient.DEFAULT_CONNECT_TIMEOUT);
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
		this.optimiezer.setOptimiezeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		this.optimiezer.setOptimiezeMergeBuffer(optimizeMergeBuffer);
	}

	protected MemcachedSessionLocator sessionLocator;

	static class ConnectFuture implements Future<Boolean> {

		private boolean connected = false;
		private boolean done = false;
		private boolean cancel = false;
		private Lock lock = new ReentrantLock();
		private Condition notDone = lock.newCondition();
		private volatile Exception exception;

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
				notDone.signal();
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
				notDone.signal();
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

	CopyOnWriteArrayList<MemcachedTCPSession> memcachedSessions; // 连接管理

	public void addSession(MemcachedTCPSession session) {
		log.warn("add session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		this.memcachedSessions.add(session);
		this.sessionLocator.updateSessionList(this.memcachedSessions);
	}

	public void removeSession(MemcachedTCPSession session) {
		log.warn("remove session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		this.memcachedSessions.remove(session);
		this.sessionLocator.updateSessionList(this.memcachedSessions);
	}

	private int sendBufferSize = 0;
	protected MemcachedProtocolHandler memcachedProtocolHandler;
	private MemcachedTCPSession session;

	public int getSendBufferSize() {
		return sendBufferSize;
	}

	public void setSendBufferSize(int sendBufferSize) {
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
				future.setException(new IOException("Connect Fail"));
			} else {
				addSession(createSession(key, (SocketChannel) (key.channel())));
				future.setConnected(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			future.setException(e);
			throw new IOException(e);
		}
	}

	protected MemcachedTCPSession createSession(SelectionKey key,
			SocketChannel socketChannel) {
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

	public Future<Boolean> connect(InetSocketAddress address)
			throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setSoTimeout(timeout);
		socketChannel.socket().setReuseAddress(reuseAddress); // 重用端口

		if (this.receiveBufferSize > 0) {
			socketChannel.socket().setReceiveBufferSize(receiveBufferSize); // 设置接收缓冲区

		}
		socketChannel.socket().bind(this.socketAddress);
		if (this.sendBufferSize > 0) {
			socketChannel.socket().setSendBufferSize(this.sendBufferSize);
		}
		ConnectFuture future = new ConnectFuture();
		if (!socketChannel.connect(address)) {
			this.reactor.registerChannel(socketChannel,
					SelectionKey.OP_CONNECT, future);
			// socketChannel.register(this.selector, SelectionKey.OP_CONNECT,
			// future);
			this.reactor.wakeup();
		} else {
			SelectionKey selectionKey = socketChannel
					.register(this.selector, 0);
			addSession(createSession(selectionKey, socketChannel));
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

	protected Session findSessionByKey(String key) {
		return sessionLocator.getSessionByKey(key);
	}

	public void setMemcachedProtocolHandler(
			MemcachedProtocolHandler memcachedProtocolHandler) {
		this.memcachedProtocolHandler = memcachedProtocolHandler;
	}

	public MemcachedProtocolHandler getMemcachedProtocolHandler() {
		return this.memcachedProtocolHandler;
	}

	public MemcachedConnector(Configuration configuration,
			MemcachedSessionLocator locator, BufferAllocator allocator) {
		super(configuration, null);
		this.memcachedSessions = new CopyOnWriteArrayList<MemcachedTCPSession>();
		this.sessionLocator = locator;
		this.sessionLocator.updateSessionList(memcachedSessions);
		this.sessionMonitor = new SessionMonitor();
		this.bufferAllocator = allocator;
		this.optimiezer = new OptimiezerImpl(this.bufferAllocator);
	}

	/**
	 * 使用扩展queue
	 */
	protected Queue<Session.WriteMessage> buildQueue() {
		return new SimpleDeque<Session.WriteMessage>(500);
	}

	public void setMergeFactor(int mergeFactor) {
		this.optimiezer.setMergeFactor(mergeFactor);
	}

	protected Session buildSession(SocketChannel sc, SelectionKey selectionKey) {
		Queue<Session.WriteMessage> queue = buildQueue();
		final SessionConfig sessionCofig = buildSessionConfig(sc, selectionKey,
				queue);
		session = new MemcachedTCPSession(sessionCofig, configuration
				.getSessionReadBufferSize(), this.optimiezer, this
				.getReadThreadCount());
		session.setMemcachedProtocolHandler(this.getMemcachedProtocolHandler());
		return session;
	}

	public BufferAllocator getByteBufferAllocator() {
		return bufferAllocator;
	}

	public void setByteBufferAllocator(BufferAllocator allocator) {
		this.bufferAllocator = allocator;
	}
}
