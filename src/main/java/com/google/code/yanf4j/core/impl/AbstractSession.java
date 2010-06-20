package com.google.code.yanf4j.core.impl;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SessionConfig;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.statistics.Statistics;

/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:04:05
 */
public abstract class AbstractSession implements Session {

	protected IoBuffer readBuffer;
	protected static final Logger log = LoggerFactory
			.getLogger(AbstractSession.class);

	protected final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	protected Queue<WriteMessage> writeQueue;

	protected volatile long sessionIdleTimeout;

	protected volatile long sessionTimeout;

	public long getSessionIdleTimeout() {
		return this.sessionIdleTimeout;
	}

	public void setSessionIdleTimeout(long sessionIdleTimeout) {
		this.sessionIdleTimeout = sessionIdleTimeout;
	}

	public long getSessionTimeout() {
		return this.sessionTimeout;
	}

	public void setSessionTimeout(long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public Queue<WriteMessage> getWriteQueue() {
		return this.writeQueue;
	}

	public Statistics getStatistics() {
		return this.statistics;
	}

	public Handler getHandler() {
		return this.handler;
	}

	public Dispatcher getDispatchMessageDispatcher() {
		return this.dispatchMessageDispatcher;
	}

	public ReentrantLock getWriteLock() {
		return this.writeLock;
	}

	protected CodecFactory.Encoder encoder;
	protected CodecFactory.Decoder decoder;

	protected volatile boolean closed;

	protected Statistics statistics;

	protected Handler handler;

	protected boolean loopback;

	public AtomicLong lastOperationTimeStamp = new AtomicLong(0);

	protected AtomicLong scheduleWritenBytes = new AtomicLong(0);

	protected final Dispatcher dispatchMessageDispatcher;
	protected volatile boolean useBlockingWrite = false;
	protected volatile boolean useBlockingRead = true;
	protected volatile boolean handleReadWriteConcurrently = true;

	public abstract void decode();

	public void updateTimeStamp() {
		this.lastOperationTimeStamp.set(System.currentTimeMillis());
	}

	public long getLastOperationTimeStamp() {
		return this.lastOperationTimeStamp.get();
	}

	public final boolean isHandleReadWriteConcurrently() {
		return this.handleReadWriteConcurrently;
	}

	public final void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	public long getScheduleWritenBytes() {
		return this.scheduleWritenBytes.get();
	}

	public CodecFactory.Encoder getEncoder() {
		return this.encoder;
	}

	public void setEncoder(CodecFactory.Encoder encoder) {
		this.encoder = encoder;
	}

	public CodecFactory.Decoder getDecoder() {
		return this.decoder;
	}

	public IoBuffer getReadBuffer() {
		return this.readBuffer;
	}

	public void setReadBuffer(IoBuffer readBuffer) {
		this.readBuffer = readBuffer;
	}

	public void setDecoder(CodecFactory.Decoder decoder) {
		this.decoder = decoder;
	}

	public final ByteOrder getReadBufferByteOrder() {
		if (this.readBuffer == null) {
			throw new IllegalStateException();
		}
		return this.readBuffer.order();
	}

	public final void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
		if (this.readBuffer == null) {
			throw new NullPointerException("Null ReadBuffer");
		}
		this.readBuffer.order(readBufferByteOrder);
	}

	// synchronized,prevent reactors invoking this method concurrently.
	protected synchronized void onIdle() {
		try {
			// check twice
			if (isIdle()) {
				updateTimeStamp();
				this.handler.onSessionIdle(this);
			}
		} catch (Throwable e) {
			onException(e);
		}
	}

	protected void onConnected() {
		try {
			this.handler.onSessionConnected(this);
		} catch (Throwable e) {
			onException(e);
		}
	}

	public void onExpired() {
		try {
			if (isExpired()) {
				this.handler.onSessionExpired(this);
			}
		} catch (Throwable e) {
			onException(e);
		}
	}

	protected abstract WriteMessage wrapMessage(Object msg,
			Future<Boolean> writeFuture);

	/**
	 * Pre-Process WriteMessage before writing to channel
	 * 
	 * @param writeMessage
	 * @return
	 */
	protected WriteMessage preprocessWriteMessage(WriteMessage writeMessage) {
		return writeMessage;
	}

	protected void dispatchReceivedMessage(final Object message) {
		if (this.dispatchMessageDispatcher == null) {
			long start = -1;
			if (this.statistics != null && this.statistics.isStatistics()) {
				start = System.currentTimeMillis();
			}
			onMessage(message, this);
			if (start != -1) {
				this.statistics.statisticsProcess(System.currentTimeMillis()
						- start);
			}
		} else {

			this.dispatchMessageDispatcher.dispatch(new Runnable() {
				public void run() {
					long start = -1;
					if (AbstractSession.this.statistics != null
							&& AbstractSession.this.statistics.isStatistics()) {
						start = System.currentTimeMillis();
					}
					onMessage(message, AbstractSession.this);
					if (start != -1) {
						AbstractSession.this.statistics
								.statisticsProcess(System.currentTimeMillis()
										- start);
					}
				}

			});
		}

	}

	private void onMessage(final Object message, Session session) {
		try {
			this.handler.onMessageReceived(session, message);
		} catch (Throwable e) {
			onException(e);
		}
	}

	public final boolean isClosed() {
		return this.closed;
	}

	public final void setClosed(boolean closed) {
		this.closed = closed;
	}

	public final void close() {
		if (isClosed()) {
			return;
		}
		setClosed(true);
		try {
			closeChannel();
			clearAttributes();
			log.debug("session closed");
		} catch (IOException e) {
			onException(e);
			log.error("Close session error", e);
		} finally {
			onClosed();
		}
	}

	protected abstract void closeChannel() throws IOException;

	public void onException(Throwable e) {
		this.handler.onExceptionCaught(this, e);
	}

	protected void onClosed() {
		try {
			this.handler.onSessionClosed(this);
		} catch (Throwable e) {
			onException(e);
		}
	}

	public void setAttribute(String key, Object value) {
		this.attributes.put(key, value);
	}

	public Object setAttributeIfAbsent(String key, Object value) {
		return this.attributes.putIfAbsent(key, value);
	}

	public void removeAttribute(String key) {
		this.attributes.remove(key);
	}

	public Object getAttribute(String key) {
		return this.attributes.get(key);
	}

	public void clearAttributes() {
		this.attributes.clear();
	}

	public synchronized void start() {
		log.debug("session started");
		onStarted();
		start0();
	}

	protected abstract void start0();

	protected void onStarted() {
		try {
			this.handler.onSessionStarted(this);
		} catch (Throwable e) {
			onException(e);
		}
	}

	protected ReentrantLock writeLock = new ReentrantLock();

	protected AtomicReference<WriteMessage> currentMessage = new AtomicReference<WriteMessage>();

	static final class FailFuture implements Future<Boolean> {

		public boolean cancel(boolean mayInterruptIfRunning) {
			return Boolean.FALSE;
		}

		public Boolean get() throws InterruptedException, ExecutionException {
			return Boolean.FALSE;
		}

		public Boolean get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return Boolean.FALSE;
		}

		public boolean isCancelled() {
			return false;
		}

		public boolean isDone() {
			return true;
		}

	}

	public Future<Boolean> asyncWrite(Object packet) {
		if (this.closed) {
			FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
			writeFuture.failure(new IOException("�����Ѿ����ر�"));
			return writeFuture;
		}
		if (this.statistics.isSendOverFlow()) {
			if (!this.handler.onSessionWriteOverFlow(this, packet)) {
				return new FailFuture();
			}
		}
		FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
		WriteMessage message = wrapMessage(packet, writeFuture);
		this.scheduleWritenBytes
				.addAndGet(message.getWriteBuffer().remaining());
		write0(message);
		return writeFuture;
	}

	public void write(Object packet) {
		if (this.closed) {
			return;
		}
		if (this.statistics.isSendOverFlow()) {
			if (!this.handler.onSessionWriteOverFlow(this, packet)) {
				return;
			}
		}
		WriteMessage message = wrapMessage(packet, null);
		this.scheduleWritenBytes
				.addAndGet(message.getWriteBuffer().remaining());
		write0(message);
	}

	protected abstract void write0(WriteMessage message);

	public final boolean isLoopbackConnection() {
		return this.loopback;
	}

	public boolean isUseBlockingWrite() {
		return this.useBlockingWrite;
	}

	public void setUseBlockingWrite(boolean useBlockingWrite) {
		this.useBlockingWrite = useBlockingWrite;
	}

	public boolean isUseBlockingRead() {
		return this.useBlockingRead;
	}

	public void setUseBlockingRead(boolean useBlockingRead) {
		this.useBlockingRead = useBlockingRead;
	}

	public void clearWriteQueue() {
		this.writeQueue.clear();
	}

	public boolean isExpired() {
		return false;
	}

	public boolean isIdle() {
		long lastOpTimestamp = this.getLastOperationTimeStamp();
		return lastOpTimestamp > 0
				&& System.currentTimeMillis() - lastOpTimestamp > this.sessionIdleTimeout;
	}

	public AbstractSession(SessionConfig sessionConfig) {
		super();
		this.lastOperationTimeStamp.set(System.currentTimeMillis());
		this.statistics = sessionConfig.statistics;
		this.handler = sessionConfig.handler;
		this.writeQueue = sessionConfig.queue;
		this.encoder = sessionConfig.codecFactory.getEncoder();
		this.decoder = sessionConfig.codecFactory.getDecoder();
		this.dispatchMessageDispatcher = sessionConfig.dispatchMessageDispatcher;
		this.handleReadWriteConcurrently = sessionConfig.handleReadWriteConcurrently;
		this.sessionTimeout = sessionConfig.sessionTimeout;
		this.sessionIdleTimeout = sessionConfig.sessionIdelTimeout;
	}

	public long transferTo(long position, long count, FileChannel target)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	public long transferFrom(long position, long count, FileChannel source)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	protected void onCreated() {
		try {
			this.handler.onSessionCreated(this);
		} catch (Throwable e) {
			onException(e);
		}
	}
}
