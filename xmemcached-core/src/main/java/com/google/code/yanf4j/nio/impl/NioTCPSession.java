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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Future;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.FutureImpl;
import com.google.code.yanf4j.core.impl.WriteMessageImpl;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.util.ByteBufferUtils;
import com.google.code.yanf4j.util.SelectorFactory;

/**
 * Nio tcp connection
 * 
 * @author dennis
 * 
 */
public class NioTCPSession extends AbstractNioSession {
	private InetSocketAddress remoteAddress;

	@Override
	public final boolean isExpired() {
		if (log.isDebugEnabled()) {
			log.debug("sessionTimeout=" + this.sessionTimeout
					+ ",this.timestamp=" + this.lastOperationTimeStamp.get()
					+ ",current=" + System.currentTimeMillis());
		}
		return this.sessionTimeout <= 0 ? false : System.currentTimeMillis()
				- this.lastOperationTimeStamp.get() >= this.sessionTimeout;
	}

	public NioTCPSession(NioSessionConfig sessionConfig, int readRecvBufferSize) {
		super(sessionConfig);
		if (this.selectableChannel != null
				&& this.getRemoteSocketAddress() != null) {
			this.loopback = this.getRemoteSocketAddress().getAddress()
					.isLoopbackAddress();
		}
		this.setReadBuffer(IoBuffer.allocate(readRecvBufferSize));
		this.onCreated();
	}

	@Override
	protected Object writeToChannel(WriteMessage message) throws IOException {
		if (message.getWriteFuture() != null && !message.isWriting()
				&& message.getWriteFuture().isCancelled()) {
			return message.getMessage();
		}
		if (message.getWriteBuffer() == null) {
			if (message.getWriteFuture() != null) {
				message.getWriteFuture().setResult(Boolean.TRUE);
			}
			return message.getMessage();
		}
		IoBuffer writeBuffer = message.getWriteBuffer();
		// begin writing
		message.writing();
		if (this.useBlockingWrite) {
			return this.blockingWrite(this.selectableChannel, message,
					writeBuffer);
		} else {
			while (true) {
				long n = this.doRealWrite(this.selectableChannel, writeBuffer);
				if (n > 0) {
					this.statistics.statisticsWrite(n);
					this.scheduleWritenBytes.addAndGet(0 - n);
				}
				if (writeBuffer == null || !writeBuffer.hasRemaining()) {
					if (message.getWriteFuture() != null) {
						message.getWriteFuture().setResult(Boolean.TRUE);
					}
					return message.getMessage();
				} else if (n == 0) {
					// have more data, but the buffer is full,
					// wait next time to write
					return null;
				}
			}
		}

	}

	public InetSocketAddress getRemoteSocketAddress() {
		if (this.remoteAddress == null) {
			this.remoteAddress = (InetSocketAddress) ((SocketChannel) this.selectableChannel)
					.socket().getRemoteSocketAddress();
		}
		return this.remoteAddress;
	}

	/**
	 * Blocking write using temp selector
	 * 
	 * @param channel
	 * @param message
	 * @param writeBuffer
	 * @return
	 * @throws IOException
	 * @throws ClosedChannelException
	 */
	protected final Object blockingWrite(SelectableChannel channel,
			WriteMessage message, IoBuffer writeBuffer) throws IOException,
			ClosedChannelException {
		SelectionKey tmpKey = null;
		Selector writeSelector = null;
		int attempts = 0;
		int bytesProduced = 0;
		try {
			while (writeBuffer.hasRemaining()) {
				long len = this.doRealWrite(channel, writeBuffer);
				if (len > 0) {
					attempts = 0;
					bytesProduced += len;
					this.statistics.statisticsWrite(len);
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
						if (attempts > 2) {
							throw new IOException("Client disconnected");
						}
					}
				}
			}
			if (!writeBuffer.hasRemaining() && message.getWriteFuture() != null) {
				message.getWriteFuture().setResult(Boolean.TRUE);
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
		this.scheduleWritenBytes.addAndGet(0 - bytesProduced);
		return message.getMessage();
	}

	@Override
	protected WriteMessage wrapMessage(Object msg, Future<Boolean> writeFuture) {
		WriteMessage message = new WriteMessageImpl(msg,
				(FutureImpl<Boolean>) writeFuture);
		if (message.getWriteBuffer() == null) {
			message.setWriteBuffer(this.encoder.encode(message.getMessage(),
					this));
		}
		return message;
	}

	@Override
	protected void readFromBuffer() {
		if (!this.readBuffer.hasRemaining()) {
			if (this.readBuffer.capacity() < Configuration.MAX_READ_BUFFER_SIZE) {
				this.readBuffer = IoBuffer.wrap(ByteBufferUtils
						.increaseBufferCapatity(this.readBuffer.buf()));
			} else {
				// buffer's capacity is greater than maxium
				return;
			}
		}
		if (this.closed) {
			return;
		}
		int n = -1;
		int readCount = 0;
		try {
			while ((n = ((ReadableByteChannel) this.selectableChannel)
					.read(this.readBuffer.buf())) > 0) {
				readCount += n;
			}
			if (readCount > 0) {
				decodeAndDispatch();
			} else if (readCount == 0
					&& !((SocketChannel) this.selectableChannel).socket()
							.isInputShutdown() && this.useBlockingRead) {
				n = this.blockingRead();
				if (n > 0) {
					readCount += n;
				}
			}
			if (n < 0) { // Connection closed
				this.close();
			} else {
				this.selectorManager.registerSession(this,
						EventType.ENABLE_READ);
			}
			if (log.isDebugEnabled()) {
				log.debug("read " + readCount + " bytes from channel");
			}
		} catch (ClosedChannelException e) {
			// ignore exception
			this.close();
		} catch (Throwable e) {
			this.onException(e);
			this.close();
		}
	}

	private void decodeAndDispatch() {
		updateTimeStamp();
		this.readBuffer.flip();
		this.decode();
		this.readBuffer.compact();
	}

	/**
	 * Blocking read using temp selector
	 * 
	 * @return
	 * @throws ClosedChannelException
	 * @throws IOException
	 */
	protected final int blockingRead() throws ClosedChannelException,
			IOException {
		int n = 0;
		int readCount = 0;
		Selector readSelector = SelectorFactory.getSelector();
		SelectionKey tmpKey = null;
		try {
			if (this.selectableChannel.isOpen()) {
				tmpKey = this.selectableChannel.register(readSelector, 0);
				tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
				int code = readSelector.select(500);
				tmpKey.interestOps(tmpKey.interestOps() & ~SelectionKey.OP_READ);
				if (code > 0) {
					do {
						n = ((ReadableByteChannel) this.selectableChannel)
								.read(this.readBuffer.buf());
						readCount += n;
						if (log.isDebugEnabled()) {
							log.debug("use temp selector read " + n + " bytes");
						}
					} while (n > 0 && this.readBuffer.hasRemaining());
					if (readCount > 0) {
						decodeAndDispatch();
					}
				}
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
		return readCount;
	}

	/**
	 * Decode buffer
	 */
	@Override
	public void decode() {
		Object message;
		int size = this.readBuffer.remaining();
		while (this.readBuffer.hasRemaining()) {
			try {
				message = this.decoder.decode(this.readBuffer, this);
				if (message == null) {
					break;
				} else {
					if (this.statistics.isStatistics()) {
						this.statistics.statisticsRead(size
								- this.readBuffer.remaining());
						size = this.readBuffer.remaining();
					}
				}
				this.dispatchReceivedMessage(message);
			} catch (Exception e) {
				this.onException(e);
				log.error("Decode error", e);
				super.close();
				break;
			}
		}
	}

	public Socket socket() {
		return ((SocketChannel) this.selectableChannel).socket();
	}

	@Override
	protected final void closeChannel() throws IOException {
		this.flush0();
		// try to close output first
		Socket socket = ((SocketChannel) this.selectableChannel).socket();
		try {
			if (!socket.isClosed() && !socket.isOutputShutdown()) {
				socket.shutdownOutput();
			}
			if (!socket.isClosed() && !socket.isInputShutdown()) {
				socket.shutdownInput();
			}
		} catch (Exception e) {
		}
		try {
			socket.close();
		} catch (Exception e) {

		}
		this.unregisterSession();
		this.unregisterChannel();
	}

}
