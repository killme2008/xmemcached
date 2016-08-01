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
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import com.google.code.yanf4j.util.LinkedTransferQueue;

/**
 * Base connection
 * 
 * @author dennis
 * 
 */
public abstract class AbstractSession implements Session {

	protected IoBuffer readBuffer;
	protected static final Logger log = LoggerFactory
			.getLogger(AbstractSession.class);

	protected final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	protected Queue<WriteMessage> writeQueue;

	protected long sessionIdleTimeout;

	protected long sessionTimeout;

	public long getSessionIdleTimeout() {
		return sessionIdleTimeout;
	}

	public void setSessionIdleTimeout(long sessionIdleTimeout) {
		this.sessionIdleTimeout = sessionIdleTimeout;
	}

	public long getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public Queue<WriteMessage> getWriteQueue() {
		return writeQueue;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public Handler getHandler() {
		return handler;
	}

	public Dispatcher getDispatchMessageDispatcher() {
		return dispatchMessageDispatcher;
	}

	public ReentrantLock getWriteLock() {
		return writeLock;
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
	protected boolean useBlockingWrite = false;
	protected boolean useBlockingRead = true;
	protected boolean handleReadWriteConcurrently = true;

	public abstract void decode();

	public void updateTimeStamp() {
		lastOperationTimeStamp.set(System.currentTimeMillis());
	}

	public long getLastOperationTimeStamp() {
		return lastOperationTimeStamp.get();
	}

	public final boolean isHandleReadWriteConcurrently() {
		return handleReadWriteConcurrently;
	}

	public final void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	public long getScheduleWritenBytes() {
		return scheduleWritenBytes.get();
	}

	public CodecFactory.Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(CodecFactory.Encoder encoder) {
		this.encoder = encoder;
	}

	public CodecFactory.Decoder getDecoder() {
		return decoder;
	}

	public IoBuffer getReadBuffer() {
		return readBuffer;
	}

	public void setReadBuffer(IoBuffer readBuffer) {
		this.readBuffer = readBuffer;
	}

	public void setDecoder(CodecFactory.Decoder decoder) {
		this.decoder = decoder;
	}

	public final ByteOrder getReadBufferByteOrder() {
		if (readBuffer == null) {
			throw new IllegalStateException();
		}
		return readBuffer.order();
	}

	public final void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
		if (readBuffer == null) {
			throw new NullPointerException("Null ReadBuffer");
		}
		readBuffer.order(readBufferByteOrder);
	}

	// synchronized,prevent reactors invoking this method concurrently.
	protected synchronized void onIdle() {
		try {
			// check twice
			if (isIdle()) {
				updateTimeStamp();
				handler.onSessionIdle(this);
			}
		} catch (Throwable e) {
			onException(e);
		}
	}

	protected void onConnected() {
		try {
			handler.onSessionConnected(this);
		} catch (Throwable e) {
			onException(e);
		}
	}

	public void onExpired() {
		try {
			if (isExpired()) {
				handler.onSessionExpired(this);
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
		if (dispatchMessageDispatcher == null) {
			long start = -1;
			if (statistics != null && statistics.isStatistics()) {
				start = System.currentTimeMillis();
			}
			onMessage(message, this);
			if (start != -1) {
				statistics
						.statisticsProcess(System.currentTimeMillis() - start);
			}
		} else {

			dispatchMessageDispatcher.dispatch(new Runnable() {
				public void run() {
					long start = -1;
					if (statistics != null && statistics.isStatistics()) {
						start = System.currentTimeMillis();
					}
					onMessage(message, AbstractSession.this);
					if (start != -1) {
						statistics.statisticsProcess(System.currentTimeMillis()
								- start);
					}
				}

			});
		}

	}

	private void onMessage(final Object message, Session session) {
		try {
			handler.onMessageReceived(session, message);
		} catch (Throwable e) {
			onException(e);
		}
	}

	public final boolean isClosed() {
		return closed;
	}

	public final void setClosed(boolean closed) {
		this.closed = closed;
	}

	public void close() {
		synchronized (this) {
			if (isClosed()) {
				return;
			}
			setClosed(true);
		}
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
		handler.onExceptionCaught(this, e);
	}

	protected void onClosed() {
		try {
			handler.onSessionClosed(this);
		} catch (Throwable e) {
			onException(e);
		}
	}

	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	public Object setAttributeIfAbsent(String key, Object value) {
		return attributes.putIfAbsent(key, value);
	}

	public void removeAttribute(String key) {
		attributes.remove(key);
	}

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public void clearAttributes() {
		attributes.clear();
	}

	public synchronized void start() {
		log.debug("session started");
		onStarted();
		start0();
	}

	protected abstract void start0();

	protected void onStarted() {
		try {
			handler.onSessionStarted(this);
		} catch (Throwable e) {
			onException(e);
		}
	}

	protected ReentrantLock writeLock = new ReentrantLock();

	protected AtomicReference<WriteMessage> currentMessage = new LinkedTransferQueue.PaddedAtomicReference<WriteMessage>(null);

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

	public void write(Object packet) {
		if (closed) {
			return;
		}
		WriteMessage message = wrapMessage(packet, null);
		scheduleWritenBytes.addAndGet(message.getWriteBuffer().remaining());
		writeFromUserCode(message);
	}

	public abstract void writeFromUserCode(WriteMessage message);


	public final boolean isLoopbackConnection() {
		return loopback;
	}

	public boolean isUseBlockingWrite() {
		return useBlockingWrite;
	}

	public void setUseBlockingWrite(boolean useBlockingWrite) {
		this.useBlockingWrite = useBlockingWrite;
	}

	public boolean isUseBlockingRead() {
		return useBlockingRead;
	}

	public void setUseBlockingRead(boolean useBlockingRead) {
		this.useBlockingRead = useBlockingRead;
	}

	public void clearWriteQueue() {
		writeQueue.clear();
	}

	public boolean isExpired() {
		return false;
	}

	public boolean isIdle() {
		long lastOpTimestamp = getLastOperationTimeStamp();
		return lastOpTimestamp > 0
				&& System.currentTimeMillis() - lastOpTimestamp > sessionIdleTimeout;
	}

	public AbstractSession(SessionConfig sessionConfig) {
		super();
		lastOperationTimeStamp.set(System.currentTimeMillis());
		statistics = sessionConfig.statistics;
		handler = sessionConfig.handler;
		writeQueue = sessionConfig.queue;
		encoder = sessionConfig.codecFactory.getEncoder();
		decoder = sessionConfig.codecFactory.getDecoder();
		dispatchMessageDispatcher = sessionConfig.dispatchMessageDispatcher;
		handleReadWriteConcurrently = sessionConfig.handleReadWriteConcurrently;
		sessionTimeout = sessionConfig.sessionTimeout;
		sessionIdleTimeout = sessionConfig.sessionIdelTimeout;
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
			handler.onSessionCreated(this);
		} catch (Throwable e) {
			onException(e);
		}
	}
}
