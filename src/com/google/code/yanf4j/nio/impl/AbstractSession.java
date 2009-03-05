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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.nio.util.SelectorFactory;
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.statistics.impl.DefaultStatistics;
import com.google.code.yanf4j.util.Queue;

/**
 * 会话抽象基类
 * 
 * @author dennis
 * 
 */
public abstract class AbstractSession implements Session {

	public enum SessionStatus {
		NULL, READING, WRITING, IDLE, INITIALIZE, CLOSING, CLOSED
	}

	protected volatile SessionStatus sessionStatus = SessionStatus.NULL;

	protected SelectionKey selectionKey;

	protected ByteBuffer readBuffer;

	protected static final Log log = LogFactory.getLog(DefaultTCPSession.class);

	protected Object attachment = null;

	@SuppressWarnings("unchecked")
	protected Queue writeQueue;

	protected SessionEventManager sessionEventManager;

	@SuppressWarnings("unchecked")
	protected CodecFactory.Encoder encoder;

	@SuppressWarnings("unchecked")
	protected CodecFactory.Decoder decoder;

	protected volatile boolean closed;

	protected Statistics statistics;

	@SuppressWarnings("unchecked")
	protected Handler handler;

	protected SelectableChannel selectableChannel;

	protected volatile boolean useBlockingWrite = false;

	protected volatile boolean useBlockingRead = true;

	protected volatile boolean handleReadWriteConcurrently;

	public boolean isHandleReadWriteConcurrently() {
		return handleReadWriteConcurrently;
	}

	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	@SuppressWarnings("unchecked")
	public CodecFactory.Encoder getEncoder() {
		return encoder;
	}

	@SuppressWarnings("unchecked")
	public void setEncoder(CodecFactory.Encoder encoder) {
		this.encoder = encoder;
	}

	@SuppressWarnings("unchecked")
	public CodecFactory.Decoder getDecoder() {
		return decoder;
	}

	@SuppressWarnings("unchecked")
	public void setDecoder(CodecFactory.Decoder decoder) {
		this.decoder = decoder;
	}

	public ByteOrder getReadBufferByteOrder() {
		if (this.readBuffer == null)
			throw new IllegalStateException();
		return this.readBuffer.order();
	}

	public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
		this.readBuffer.order(readBufferByteOrder);
	}

	protected void enableRead() {
		if (selectionKey.isValid())
			selectionKey.interestOps(selectionKey.interestOps()
					| SelectionKey.OP_READ);
	}

	public void onEvent(EventType event, java.nio.channels.Selector selector) {
		if (this.closed)
			return;
		switch (event) {
		case EXPIRED:
			onExpired();
			break;
		case WRITEABLE:
			onWrite();
			break;
		case READABLE:
			onRead();
			break;
		case ENABLE_WRITE:
			enableWrite();
			break;
		case ENABLE_READ:
			enableRead();
			break;
		case IDLE:
			onIdle();
			break;
		case CONNECTED:
			onConnected();
			break;
		default:
			break;
		}
	}

	protected void onIdle() {
		this.handler.onIdle(this);
	}

	protected void onConnected() {
		this.handler.onConnected(this);
	}

	protected void onRead() {
		setSessionStatus(SessionStatus.READING);
		readFromBuffer();
	}

	public void onExpired() {
		handler.onExpired(this);
	}

	protected abstract Object writeToChannel(SelectableChannel sc,
			WriteMessage msg) throws ClosedChannelException, IOException;

	protected abstract void readFromBuffer();

	protected abstract Object wrapMessage(Object msg);

	@SuppressWarnings("unchecked")
	protected synchronized void onWrite() {
		WriteMessage msg = null;
		try {
			if (getSessionStatus().equals(SessionStatus.WRITING)) // 用户可能正在调用flush方法
				return;
			if (getSessionStatus().equals(SessionStatus.READING) // 不允许读写并行
					&& !handleReadWriteConcurrently)
				return;
			selectionKey.interestOps(selectionKey.interestOps()
					& ~SelectionKey.OP_WRITE);
			setSessionStatus(SessionStatus.WRITING);
			boolean writeComplete = false;
			while (true) {
				msg = (WriteMessage) writeQueue.peek();
				if (msg == null) {
					writeComplete = true;
					break;
				}
				Object message = writeToChannel(selectableChannel, msg);
				if (message != null) { // write complete
					writeQueue.pop(); // remove message
					handler.onMessageSent(this, msg.message);
					msg.buffers = null; // gc friendly
					msg.message = null;
					msg = null;
				} else { // not write complete, but write buffer is full
					break;
				}
			}
			if (!writeComplete) {
				sessionEventManager.register(this, EventType.ENABLE_WRITE); // listening
				// OP_WRITE
			}
			setSessionStatus(SessionStatus.IDLE);
		} catch (CancelledKeyException cke) {
			log.error(cke, cke);
			handler.onException(this, cke);
			close();

		} catch (ClosedChannelException cce) {
			log.error(cce, cce);
			handler.onException(this, cce);
			close();
		} catch (IOException ioe) {
			log.error(msg.message);
			StringBuffer bufMsg = new StringBuffer("send buffers:\n[");
			for (ByteBuffer buff : msg.buffers) {
				bufMsg.append("buff:position=").append(buff.position()).append(
						",limit=").append(buff.limit()).append(",capacity=")
						.append(buff.capacity()).append("\n");
			}
			bufMsg.append("]");
			log.error(bufMsg.toString());
			log.error(ioe, ioe);
			handler.onException(this, ioe);
			close();
		} catch (Exception e) {
			handler.onException(this, e);
			log.error(e, e);
			close();
		}
	}

	protected void enableWrite() {
		if (selectionKey.isValid())
			selectionKey.interestOps(selectionKey.interestOps()
					| SelectionKey.OP_WRITE);
	}

	protected long doRealWrite(SelectableChannel channel, ByteBuffer[] buffer)
			throws IOException {
		if (log.isDebugEnabled()) {
			StringBuffer bufMsg = new StringBuffer("send buffers:\n[\n");
			for (ByteBuffer buff : buffer) {
				bufMsg.append(" buffer:position=").append(buff.position()).append(
						",limit=").append(buff.limit()).append(",capacity=")
						.append(buff.capacity()).append("\n");

			}
			bufMsg.append("]");
			log.debug(bufMsg.toString());
		}
		return ((GatheringByteChannel) channel).write(buffer);
	}

	@SuppressWarnings("unchecked")
	protected void dispatchReceivedMessage(Object pkt) {
		long start = -1;
		if (!(this.statistics instanceof DefaultStatistics))
			start = System.currentTimeMillis();
		handler.onReceive(this, pkt);
		if (start != -1) {
			this.statistics.statisticsProcess(System.currentTimeMillis()
					- start);
		}
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
			selectableChannel.close();
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

	public void attach(Object obj) {
		this.attachment = obj;
	}

	public Object attachment() {
		return this.attachment;
	}

	public boolean isClose() {
		return closed;
	}

	public void start() {
		log.debug("session started");
		// 注册到reactor
		this.sessionEventManager.register(this, EventType.REGISTER);
		setSessionStatus(SessionStatus.IDLE);
		handler.onSessionStarted(this);
	}

	@SuppressWarnings("unchecked")
	public boolean send(Object msg) throws InterruptedException {
		if (isClose())
			return false;
		Object message = wrapMessage(msg);
		writeQueue.getLock().lock();
		try {
			if (writeQueue.isEmpty()) {
				if (writeQueue.push(message)) {
					sessionEventManager.register(this, EventType.ENABLE_WRITE); // 列表为空，注册监听写事件
					return true;
				} else
					return false;
			} else {
				return writeQueue.push(message);
			}
		} finally {
			writeQueue.getLock().unlock();
			selectionKey.selector().wakeup();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean send(Object msg, long timeout) throws InterruptedException {
		if (isClose())
			return false;
		Object message = wrapMessage(msg);
		writeQueue.getLock().lock();
		try {

			if (writeQueue.isEmpty()) {
				if (writeQueue.push(message, timeout)) {
					// 列表为空，注册监听写事件
					sessionEventManager.register(this, EventType.ENABLE_WRITE);
					return true;
				} else
					return false;
			} else
				return writeQueue.push(message, timeout);
		} finally {
			writeQueue.getLock().unlock();
			selectionKey.selector().wakeup();
		}

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
		this.writeQueue.clear();
	}

	public boolean isExpired() {
		return false;
	}

	public SessionStatus getSessionStatus() {
		return sessionStatus;
	}

	public void setSessionStatus(SessionStatus sessionStatus) {
		this.sessionStatus = sessionStatus;
	}

	@SuppressWarnings("unchecked")
	public AbstractSession(SelectableChannel sc, SelectionKey sk,
			Handler handler, SessionEventManager reactor,
			CodecFactory codecFactory, Statistics statistics,
			Queue<WriteMessage> queue, boolean handleReadWriteConcurrently) {
		super();
		this.selectionKey = sk;
		this.sessionEventManager = reactor;
		this.statistics = statistics;
		this.handler = handler;
		this.selectableChannel = sc;
		this.writeQueue = queue;
		this.encoder = codecFactory.getEncoder();
		this.decoder = codecFactory.getDecoder();
		setSessionStatus(SessionStatus.INITIALIZE);
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	@SuppressWarnings("unchecked")
	public synchronized void flush() throws IOException, InterruptedException {
		if (this.closed)
			throw new IllegalStateException();
		if (getSessionStatus().equals(SessionStatus.IDLE)
				|| isReadingAndAllowWriteConcurrently()) {
			SelectionKey tmpKey = null;
			Selector writeSelector = null;
			int attempts = 0;
			writeQueue.getLock().lock();
			setSessionStatus(SessionStatus.WRITING);
			try {
				if (writeSelector == null) {
					writeSelector = SelectorFactory.getSelector();
					if (writeSelector == null) {
						return;
					}
					tmpKey = selectableChannel.register(writeSelector,
							SelectionKey.OP_WRITE);
				}
				while (true) {
					if (writeSelector.select(1000) == 0) {
						attempts++;
						if (attempts > 2) // 尝试次数超过两次，放弃
							return;
					} else
						break;
				}
				while (true) {
					WriteMessage msg = (WriteMessage) writeQueue.peek();
					if (msg == null) {
						break;
					}
					Object message = writeToChannel(selectableChannel, msg);
					if (message != null) { // write complete
						writeQueue.pop(); // remove message
						handler.onMessageSent(this, msg.message);
						msg.buffers = null; // gc friendly
						msg.message = null;
						msg = null;
					} else { // not write complete, but write buffer is full
						break;
					}
				}
			} catch (ClosedChannelException cce) {
				handler.onException(this, cce);
				log.error(cce, cce);
				close();
			} catch (IOException ioe) {
				handler.onException(this, ioe);
				log.error(ioe, ioe);
				close();
			} finally {
				if (tmpKey != null) {
					// Cancel the key.
					tmpKey.cancel();
					tmpKey = null;
				}
				if (writeSelector != null) {
					// return selector
					writeSelector.selectNow();
					SelectorFactory.returnSelector(writeSelector);
				}
				setSessionStatus(SessionStatus.IDLE);
				writeQueue.getLock().unlock();
			}
		}
	}

	private boolean isReadingAndAllowWriteConcurrently() {
		return (getSessionStatus().equals(SessionStatus.READING) && handleReadWriteConcurrently);
	}

	public long transferTo(long position, long count, FileChannel target)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	public long transferFrom(long position, long count, FileChannel source)
			throws IOException {
		throw new UnsupportedOperationException();
	}

}
