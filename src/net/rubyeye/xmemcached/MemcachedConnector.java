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

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.SimpleQueue;

/**
 * 针对memcached的连接管理类
 * 
 * @author dennis
 */
public class MemcachedConnector extends SocketChannelController {
	final BlockingQueue<InetSocketAddress> waitingQueue = new LinkedBlockingQueue<InetSocketAddress>();
	private BufferAllocator allocator;

	private SessionMonitor sessionMonitor;

	class SessionMonitor extends Thread {
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				try {
					InetSocketAddress address = waitingQueue.take();
					int tries = 0;
					// 最多重连10次
					boolean connected = false;
					while (tries < 10) {
						Future<Boolean> future = connect(address);
						tries++;
						try {
							log.warn("try to connect to "
									+ address.getHostName() + ":"
									+ address.getPort() + " for " + tries
									+ " times");
							if (!future.get(XMemcachedClient.CONNECT_TIMEOUT,
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
						waitingQueue.add(address);
						// 暂停5秒
						Thread.sleep(5000);
					}
				} catch (IOException e) {
					log.error("monitor connect error", e);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private boolean optimiezeGet = true;
	private boolean optimizeSet = false;

	public void setOptimiezeGet(boolean optimiezeGet) {
		this.optimiezeGet = optimiezeGet;
	}

	public void setoptimizeSet(boolean optimizeSet) {
		this.optimizeSet = optimizeSet;
	}

	protected MemcachedSessionLocator sessionLocator;

	static class ConnectFuture implements Future<Boolean> {

		private boolean connected = false;
		private boolean done = false;
		private boolean cancel = false;
		private CountDownLatch latch = new CountDownLatch(1);
		private Exception exception;

		public boolean isConnected() {
			return connected;
		}

		public void setConnected(boolean connected) {
			this.connected = connected;
			this.latch.countDown();
			done = true;
		}

		public Exception getException() {
			return exception;
		}

		public void setException(Exception exception) {
			this.exception = exception;
			this.latch.countDown();
			done = true;

		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			this.cancel = true;
			return cancel;
		}

		@Override
		public Boolean get() throws InterruptedException, ExecutionException {
			this.latch.await();
			if (this.exception != null) {
				throw new ExecutionException(exception);
			}
			return connected ? Boolean.TRUE : Boolean.FALSE;
		}

		@Override
		public Boolean get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			if (!this.latch.await(timeout, unit)) {
				throw new TimeoutException("connect timeout");
			}
			return connected ? Boolean.TRUE : Boolean.FALSE;
		}

		@Override
		public boolean isCancelled() {
			return cancel;
		}

		@Override
		public boolean isDone() {
			return done;
		}
	}

	CopyOnWriteArrayList<MemcachedTCPSession> memcachedSessions; // 连接管理

	public void addSession(MemcachedTCPSession session) {
		log.warn("add session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		this.memcachedSessions.add(session);
		this.sessionLocator.setSessionList(this.memcachedSessions);
	}

	public void removeSession(MemcachedTCPSession session) {
		log.warn("remove session "
				+ session.getRemoteSocketAddress().getHostName() + ":"
				+ session.getRemoteSocketAddress().getPort());
		this.memcachedSessions.remove(session);
		this.sessionLocator.setSessionList(this.memcachedSessions);
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
		if (future.isCancelled()) {
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
		selector.wakeup();
		return session;
	}

	public void addToWatingQueue(InetSocketAddress address) {
		this.waitingQueue.add(address);
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
		socketChannel.connect(address);
		Future<Boolean> future = new ConnectFuture();
		this.reactor.registerChannel(socketChannel, SelectionKey.OP_CONNECT,
				future);
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

	public void send(Command msg) throws InterruptedException,
			MemcachedException {
		Session session = findSessionByKey((String) msg.getKey());
		if (session == null) {
			throw new MemcachedException(
					"There is no avriable session at this moment");
		}
		session.send(msg);

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
		this.sessionLocator.setSessionList(memcachedSessions);
		this.sessionMonitor = new SessionMonitor();
		this.allocator = allocator;
	}

	/**
	 * 使用扩展queue
	 */
	protected Queue<Session.WriteMessage> buildQueue() {
		return new SimpleQueue<Session.WriteMessage>();
	}

	private int mergeGetsCount = 65;

	public void setGetsMergeFactor(int mergeFactor) {
		this.mergeGetsCount = mergeFactor;
	}

	protected Session buildSession(SocketChannel sc, SelectionKey selectionKey) {
		Queue<Session.WriteMessage> queue = buildQueue();
		session = new MemcachedTCPSession(sc, selectionKey, handler,
				getReactor(), getCodecFactory(), configuration
						.getSessionReadBufferSize(), statistics, queue,
				sessionTimeout, handleReadWriteConcurrently, this.optimiezeGet,
				this.optimizeSet, this.allocator);
		session.setMemcachedProtocolHandler(this.getMemcachedProtocolHandler());
		session.setMergeGetsCount(this.mergeGetsCount);
		return session;
	}

	public BufferAllocator getByteBufferAllocator() {
		return allocator;
	}

	public void setByteBufferAllocator(BufferAllocator allocator) {
		this.allocator = allocator;
	}
}
