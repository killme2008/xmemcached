package com.google.code.yanf4j.nio.impl;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.util.ByteBufferUtils;
import com.google.code.yanf4j.util.Queue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.nio.util.SelectorFactory;

/**
 * 连接会话对象,负责数据的读写，连接的管理
 * 
 * @author dennis
 */
public class DefaultTCPSession extends AbstractSession {
	private long sessionTimeout = 0;

	private volatile long timestamp = -1;

	private volatile AtomicInteger idleCalledTimes = new AtomicInteger(0);

	private void updateTimeStamp() {
		idleCalledTimes.set(0);
		this.timestamp = System.currentTimeMillis();
	}

	public boolean isExpired() {
		if (log.isDebugEnabled())
			log
					.debug("sessionTimeout=" + sessionTimeout
							+ ",this.timestamp=" + this.timestamp + ",current="
							+ System.currentTimeMillis());
		return sessionTimeout == 0 ? false
				: ((System.currentTimeMillis() - this.timestamp) >= this.sessionTimeout);
	}

	@Override
	protected void onIdle() {
		idleCalledTimes.addAndGet(1);
		super.onIdle();
	}

	@SuppressWarnings("unchecked")
	public DefaultTCPSession(SocketChannel sc, SelectionKey sk,
			Handler handler, SessionEventManager reactor,
			CodecFactory codecFactory, int readRecvBufferSize,
			Statistics statistics, Queue<WriteMessage> queue,
			long sessionTimeout, boolean handleReadWriteConcurrently) {
		super(sc, sk, handler, reactor, codecFactory, statistics, queue,
				handleReadWriteConcurrently);
		this.readBuffer = ByteBuffer.allocate(readRecvBufferSize);
		this.sessionTimeout = sessionTimeout;
		updateTimeStamp();
		this.handler.onSessionCreated(this);
	}

	@SuppressWarnings("unchecked")
	protected Object writeToChannel(SelectableChannel channel,
			WriteMessage message) throws IOException {
		updateTimeStamp();
		if (message.buffers == null)
			message.buffers = this.encoder.encode(message.message);
		ByteBuffer[] writeBuffer = message.buffers;
		if (writeBuffer == null || writeBuffer.length == 0)
			return message.message; // Write completed
		// next time to write
		if (useBlockingWrite) {
			return blockingWrite(channel, message, writeBuffer);
		} else {
			while (true) {
				long n = doRealWrite(channel, writeBuffer);
				if (n > 0)
					this.statistics.statisticsWrite(n);
				if (writeBuffer == null
						|| !ByteBufferUtils.hasRemaining(writeBuffer))
					return message.message;
				else if (n == 0) {
					// have more data, but the buffer is full,
					// wait next time to write
					return null;
				}
			}
		}

	}

	public InetSocketAddress getRemoteSocketAddress() {
		return (InetSocketAddress) ((SocketChannel) selectableChannel).socket()
				.getRemoteSocketAddress();
	}

	boolean isIdle() {
		return this.sessionStatus.equals(SessionStatus.IDLE)
				&& ((System.currentTimeMillis() - this.timestamp)
						/ Configuration.CHECK_SESSION_IDLE_INTERVAL > this.idleCalledTimes
						.get());
	}

	/**
	 * 强制写完
	 * 
	 * @param channel
	 * @param message
	 * @param writeBuffer
	 * @return
	 * @throws IOException
	 * @throws ClosedChannelException
	 */
	private Object blockingWrite(SelectableChannel channel,
			WriteMessage message, ByteBuffer[] writeBuffer) throws IOException,
			ClosedChannelException {
		SelectionKey tmpKey = null;
		Selector writeSelector = null;
		int attempts = 0;
		int bytesProduced = 0;
		try {
			while (ByteBufferUtils.hasRemaining(writeBuffer)) {
				long len = doRealWrite(channel, writeBuffer);
				if (len > 0) {
					attempts = 0;
					bytesProduced += len;
					statistics.statisticsWrite(len);
				} else {
					attempts++;
					if (writeSelector == null) {
						writeSelector = SelectorFactory.getSelector();
						if (writeSelector == null) {
							// Continue using the main one.
							continue;
						}
						tmpKey = channel.register(writeSelector,
								SelectionKey.OP_WRITE);
					}
					if (writeSelector.select(1000) == 0) {
						if (attempts > 2)
							throw new IOException("Client disconnected");
					}
				}
			}
		} finally {
			if (tmpKey != null) {
				tmpKey.cancel();
				tmpKey = null;
			}
			if (writeSelector != null) {
				// Cancel the key.
				writeSelector.selectNow();
				SelectorFactory.returnSelector(writeSelector);
			}
		}
		return message.message;
	}

	@SuppressWarnings("unchecked")
	protected Object wrapMessage(Object msg) {
		WriteMessage message = new WriteMessage(msg);
		message.buffers = this.encoder.encode(message.message);
		return message;
	}

	protected void readFromBuffer() {
		updateTimeStamp();
		if (!readBuffer.hasRemaining()) { // have no room for contain new data
			readBuffer = ByteBufferUtils.increaseBufferCapatity(readBuffer);
		}
		if (closed)
			return;
		int n = -1;
		int readCount = 0;
		try {
			while ((n = ((ReadableByteChannel) selectableChannel)
					.read(readBuffer)) > 0) {
				readCount += n;
			}
			if (readCount > 0) {
				readBuffer.flip();
				decode();
				readBuffer.compact();
			} else if (readCount == 0
					&& !((SocketChannel) selectableChannel).socket()
							.isInputShutdown() && useBlockingRead) {
				n = blockingRead();
			}
			if (n < 0) { // Connection closed
				close();
			} else {
				sessionEventManager.register(this, EventType.ENABLE_READ);
			}
			setSessionStatus(SessionStatus.IDLE);
		} catch (IOException e) {
			log.error(e);
			handler.onException(this, e);
			super.close();
		} catch (Throwable e) {
			log.error(e);
			handler.onException(this, e);
			super.close();
		}
	}

	int blockingRead() throws ClosedChannelException, IOException {
		log.debug("use temp selector for blocking read");
		int n = -1;
		Selector readSelector = SelectorFactory.getSelector();
		SelectionKey tmpKey = null;
		try {
			tmpKey = selectableChannel.register(readSelector, 0);
			tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
			int code = readSelector.select(1000);
			tmpKey.interestOps(tmpKey.interestOps() & (~SelectionKey.OP_READ));
			if (code > 0) {
				do {
					n = ((ReadableByteChannel) selectableChannel)
							.read(readBuffer);
					log.debug("use temp selector read " + n + " bytes");
				} while (n > 0 && readBuffer.hasRemaining());
				readBuffer.flip();
				decode();
				readBuffer.compact();
			}
		} finally {
			if (tmpKey != null) {
				tmpKey.cancel();
				tmpKey = null;
			}
			if (readSelector != null) {
				// Cancel the key.
				readSelector.selectNow();
				SelectorFactory.returnSelector(readSelector);
			}
		}
		return n;
	}

	/**
	 * 解码，产生message，调用处理器处理
	 */
	@SuppressWarnings("unchecked")
	public void decode() {
		Object pkt;
		int size = readBuffer.remaining();
		while (readBuffer.hasRemaining()) {
			try {
				pkt = this.decoder.decode(readBuffer);
				if (pkt == null) {
					break;
				} else {
					if (statistics.isStatistics()) {
						statistics
								.statisticsRead(size - readBuffer.remaining());
						size = readBuffer.remaining();
					}
				}
				dispatchReceivedMessage(pkt);
			} catch (Exception e) {
				handler.onException(this, e);
				log.error(e, e);
				super.close();
				break;
			}
		}
	}

	public Socket getSocket() {
		return ((SocketChannel) this.selectableChannel).socket();
	}

	@Override
	public long transferTo(long position, long count, FileChannel target)
			throws IOException {
		if (getSessionStatus().equals(SessionStatus.READING))
			return 0;
		if (((SocketChannel) selectableChannel).socket().isInputShutdown())
			return -1;
		return target.transferFrom(
				(ReadableByteChannel) this.selectableChannel, position, count);
	}

	@Override
	public long transferFrom(long position, long count, FileChannel source)
			throws IOException {
		if (getSessionStatus().equals(SessionStatus.WRITING))
			return 0;
		if (((SocketChannel) selectableChannel).socket().isOutputShutdown())
			return -1;
		return source.transferTo(position, count,
				(WritableByteChannel) this.selectableChannel);
	}

	/**
	 * 关闭连接
	 */
	public void close() {
		if (closed)
			return;
		closed = true;
		setSessionStatus(SessionStatus.CLOSING);
		try {
			handler.onSessionClosed(this);
			((SocketChannel) selectableChannel).socket().shutdownOutput();
			((SocketChannel) selectableChannel).socket().close();
			this.attachment = null;
			log.debug("session closed");
		} catch (IOException e) {
			handler.onException(this, e);
			log.error(e, e);
		} finally {
			selectionKey.attach(null);
			selectionKey.cancel();
			this.sessionEventManager.register(this, EventType.UNREGISTER);
			setSessionStatus(SessionStatus.CLOSED);
		}
	}

}
