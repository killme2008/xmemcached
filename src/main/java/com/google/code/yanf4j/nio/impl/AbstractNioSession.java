package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.AbstractSession;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.util.SelectorFactory;

/**
 * Base nio session
 * 
 * @author dennis
 * 
 */
public abstract class AbstractNioSession extends AbstractSession implements
		NioSession {

	public SelectableChannel channel() {
		return this.selectableChannel;
	}

	protected SelectorManager selectorManager;
	protected SelectableChannel selectableChannel;

	public AbstractNioSession(NioSessionConfig sessionConfig) {
		super(sessionConfig);
		this.selectorManager = sessionConfig.selectorManager;
		this.selectableChannel = sessionConfig.selectableChannel;
	}

	public final void enableRead(Selector selector) {
		SelectionKey key = this.selectableChannel.keyFor(selector);
		if (key != null && key.isValid()) {
			interestRead(key);
		} else {
			try {
				this.selectableChannel.register(selector, SelectionKey.OP_READ,
						this);
			} catch (ClosedChannelException e) {
				onException(e);
			} catch (CancelledKeyException e) {
				onException(e);
				this.selectorManager.registerSession(this,
						EventType.ENABLE_READ);
			}
		}
	}

	private void interestRead(SelectionKey key) {
		if (key.attachment() == null) {
			key.attach(this);
		}
		key.interestOps(key.interestOps() | SelectionKey.OP_READ);
	}

	@Override
	protected void start0() {
		registerSession();
	}

	public InetAddress getLocalAddress() {
		return ((SocketChannel) this.selectableChannel).socket()
				.getLocalAddress();
	}

	protected abstract Object writeToChannel(WriteMessage msg)
			throws ClosedChannelException, IOException;

	protected void onWrite(SelectionKey key) {
		boolean isLockedByMe = false;
		if (this.currentMessage.get() == null) {
			// get next message
			WriteMessage nextMessage = this.writeQueue.peek();
			if (nextMessage != null && this.writeLock.tryLock()) {
				if (!this.writeQueue.isEmpty()
						&& this.currentMessage.compareAndSet(null, nextMessage)) {
					this.writeQueue.remove();
				}
			} else {
				return;
			}
		} else if (!this.writeLock.tryLock()) {
			return;
		}
		updateTimeStamp();

		isLockedByMe = true;
		WriteMessage currentMessage = null;
		// make read/write fail, write/read=3/2
		final long maxWritten = this.readBuffer.capacity()
				+ this.readBuffer.capacity() >>> 1;
		try {
			long written = 0;
			while (this.currentMessage.get() != null) {
				currentMessage = this.currentMessage.get();
				currentMessage = preprocessWriteMessage(currentMessage);
				this.currentMessage.set(currentMessage);
				long before = this.currentMessage.get().getWriteBuffer()
						.remaining();
				Object writeResult = null;

				if (written < maxWritten) {
					writeResult = writeToChannel(currentMessage);
					written += (this.currentMessage.get().getWriteBuffer()
							.remaining() - before);
				} else {
					// wait for next time to write
				}
				// write complete
				if (writeResult != null) {
					this.currentMessage.set(this.writeQueue.poll());
					this.handler.onMessageSent(this, currentMessage
							.getMessage());
					// try to get next message
					if (this.currentMessage.get() == null) {
						if (isLockedByMe) {
							isLockedByMe = false;
							this.writeLock.unlock();
						}
						// get next message
						WriteMessage nextMessage = this.writeQueue.peek();
						if (nextMessage != null && this.writeLock.tryLock()) {
							isLockedByMe = true;
							if (!this.writeQueue.isEmpty()
									&& this.currentMessage.compareAndSet(null,
											nextMessage)) {
								this.writeQueue.remove();
							}
							continue;
						} else {
							break;
						}
					}
				} else {
					// does't write complete
					if (isLockedByMe) {
						isLockedByMe = false;
						this.writeLock.unlock();
					}
					// register OP_WRITE event
					this.selectorManager.registerSession(this,
							EventType.ENABLE_WRITE);
					break;
				}
			}
		} catch (IOException e) {
			this.handler.onExceptionCaught(this, e);
			if (currentMessage != null
					&& currentMessage.getWriteFuture() != null) {
				currentMessage.getWriteFuture().failure(e);
			}
			if (isLockedByMe) {
				isLockedByMe = false;
				this.writeLock.unlock();
			}
			close();
		} finally {
			if (isLockedByMe) {
				this.writeLock.unlock();
			}
		}
	}

	public final void enableWrite(Selector selector) {
		SelectionKey key = this.selectableChannel.keyFor(selector);
		if (key != null && key.isValid()) {
			interestWrite(key);
		} else {
			try {
				this.selectableChannel.register(selector,
						SelectionKey.OP_WRITE, this);
			} catch (ClosedChannelException e) {
				onException(e);
			} catch (CancelledKeyException e) {
				onException(e);
				this.selectorManager.registerSession(this,
						EventType.ENABLE_READ);
			}
		}
	}

	private void interestWrite(SelectionKey key) {
		if (key.attachment() == null) {
			key.attach(this);
		}
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
	}

	protected void onRead(SelectionKey key) {
		updateTimeStamp();
		readFromBuffer();
	}

	protected abstract void readFromBuffer();

	@Override
	protected void closeChannel() throws IOException {
		flush0();
		unregisterSession();
		unregisterChannel();
	}

	protected final void unregisterChannel() throws IOException {
		this.writeLock.lock();
		try {
			if (this.getAttribute(SelectorManager.REACTOR_ATTRIBUTE) != null) {
				((Reactor) (this
						.getAttribute(SelectorManager.REACTOR_ATTRIBUTE)))
						.unregisterChannel(this.selectableChannel);
			}
			if (this.selectableChannel.isOpen()) {
				this.selectableChannel.close();
			}
		} finally {
			this.writeLock.unlock();
		}
	}

	protected final void registerSession() {
		this.selectorManager.registerSession(this, EventType.REGISTER);
	}

	protected void unregisterSession() {
		this.selectorManager.registerSession(this, EventType.UNREGISTER);
	}

	@Override
	protected final void write0(WriteMessage message) {
		boolean isLockedByMe = false;
		Object writeResult = null;
		try {
			// No message is writing
			if (this.currentMessage.get() == null && this.writeLock.tryLock()) {
				isLockedByMe = true;
				// try to write current message
				if (this.currentMessage.compareAndSet(null, message)) {
					message = preprocessWriteMessage(message);
					this.currentMessage.set(message);
					try {
						writeResult = writeToChannel(message);
					} catch (IOException e) {
						if (message.getWriteFuture() != null) {
							message.getWriteFuture().failure(e);
						}
						close();
					}
				} else {
					isLockedByMe = false;
					this.writeLock.unlock();
				}
			}
			// lock success,and write complete
			if (isLockedByMe && writeResult != null) {
				this.handler.onMessageSent(this, message.getMessage());
				// get next message
				WriteMessage nextElement = this.writeQueue.poll();
				if (nextElement != null) {
					this.currentMessage.set(nextElement);
					isLockedByMe = false;
					this.writeLock.unlock();
					// next message is not null,register OP_WRITE
					this.selectorManager.registerSession(this,
							EventType.ENABLE_WRITE);
				} else {
					this.currentMessage.set(null);
					isLockedByMe = false;
					this.writeLock.unlock();
					// try again to get next message
					if (this.writeQueue.peek() != null) {
						this.selectorManager.registerSession(this,
								EventType.ENABLE_WRITE);
					}
				}
			} else {
				// lock fail
				boolean isRegisterForWriting = false;
				if (this.currentMessage.get() != message) {
					// current message is not this message,add it to queue
					this.writeQueue.offer(message);
					// register OP_WRITE
					if (!this.writeLock.isLocked()) {
						isRegisterForWriting = true;
					}
				} else {
					// Current message is this message,it doesn't write complete
					isRegisterForWriting = true;
					if (isLockedByMe) {
						isLockedByMe = false;
						this.writeLock.unlock();
					}
				}
				// register OP_WRITE
				if (isRegisterForWriting) {
					this.selectorManager.registerSession(this,
							EventType.ENABLE_WRITE);
				}
			}
		} finally {
			// release lock finally
			if (isLockedByMe) {
				this.writeLock.unlock();
			}
		}
	}

	public void flush() {
		if (isClosed()) {
			return;
		}
		flush0();
	}

	protected final void flush0() {
		SelectionKey tmpKey = null;
		Selector writeSelector = null;
		int attempts = 0;
		try {
			while (true) {
				if (writeSelector == null) {
					writeSelector = SelectorFactory.getSelector();
					if (writeSelector == null) {
						return;
					}
					tmpKey = this.selectableChannel.register(writeSelector,
							SelectionKey.OP_WRITE);
				}
				if (writeSelector.select(1000) == 0) {
					attempts++;
					if (attempts > 2)
					{
						return;
					}
				} else {
					break;
				}
			}
			onWrite(this.selectableChannel.keyFor(writeSelector));
		} catch (ClosedChannelException cce) {
			onException(cce);
			log.error("Flush error", cce);
			close();
		} catch (IOException ioe) {
			onException(ioe);
			log.error("Flush error", ioe);
			close();
		} finally {
			if (tmpKey != null) {
				// Cancel the key.
				tmpKey.cancel();
				tmpKey = null;
			}
			if (writeSelector != null) {
				try {
					writeSelector.selectNow();
				} catch (IOException e) {
					log.error("Temp selector selectNow error", e);
				}
				// return selector
				SelectorFactory.returnSelector(writeSelector);
			}
		}
	}

	protected final long doRealWrite(SelectableChannel channel, IoBuffer buffer)
			throws IOException {
		if (log.isDebugEnabled()) {
			StringBuffer bufMsg = new StringBuffer("send buffers:\n[\n");
			final ByteBuffer buff = buffer.buf();
			bufMsg.append(" buffer:position=").append(buff.position()).append(
					",limit=").append(buff.limit()).append(",capacity=")
					.append(buff.capacity()).append("\n");

			bufMsg.append("]");
			log.debug(bufMsg.toString());
		}
		return ((WritableByteChannel) channel).write(buffer.buf());
	}

	/**
	 * �ɷ�IO�¼�
	 */
	public final void onEvent(EventType event, Selector selector) {
		if (isClosed()) {
			return;
		}
		SelectionKey key = this.selectableChannel.keyFor(selector);

		switch (event) {
		case EXPIRED:
			onExpired();
			break;
		case WRITEABLE:
			onWrite(key);
			break;
		case READABLE:
			onRead(key);
			break;
		case ENABLE_WRITE:
			enableWrite(selector);
			break;
		case ENABLE_READ:
			enableRead(selector);
			break;
		case IDLE:
			onIdle();
			break;
		case CONNECTED:
			onConnected();
			break;
		default:
			log.error("Unknown event:" + event.name());
			break;
		}
	}
}
