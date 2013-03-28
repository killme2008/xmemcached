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
 * Abstract nio session
 * 
 * @author dennis
 * 
 */
public abstract class AbstractNioSession extends AbstractSession implements
		NioSession {

	public SelectableChannel channel() {
		return selectableChannel;
	}

	protected SelectorManager selectorManager;
	protected SelectableChannel selectableChannel;

	public AbstractNioSession(NioSessionConfig sessionConfig) {
		super(sessionConfig);
		selectorManager = sessionConfig.selectorManager;
		selectableChannel = sessionConfig.selectableChannel;
	}

	public final void enableRead(Selector selector) {
		SelectionKey key = selectableChannel.keyFor(selector);
		if (key != null && key.isValid()) {
			interestRead(key);
		} else {
			try {
				selectableChannel
						.register(selector, SelectionKey.OP_READ, this);
			} catch (ClosedChannelException e) {
				// ignore
			} catch (CancelledKeyException e) {
				// ignore
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
		return ((SocketChannel) selectableChannel).socket().getLocalAddress();
	}

	protected abstract Object writeToChannel(WriteMessage msg)
			throws ClosedChannelException, IOException;

	protected void onWrite(SelectionKey key) {
		boolean isLockedByMe = false;
		if (currentMessage.get() == null) {
			// get next message
			WriteMessage nextMessage = writeQueue.peek();
			if (nextMessage != null && writeLock.tryLock()) {
				if (!writeQueue.isEmpty()
						&& currentMessage.compareAndSet(null, nextMessage)) {
					writeQueue.remove();
				}
			} else {
				return;
			}
		} else if (!writeLock.tryLock()) {
			return;
		}

		isLockedByMe = true;
		WriteMessage currentMessage = null;
		// make read/write fail, write/read=3/2
		final long maxWritten = readBuffer.capacity() + readBuffer.capacity() >>> 1;
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
					written += this.currentMessage.get().getWriteBuffer()
							.remaining()
							- before;
				} else {
					// wait for next time to write
				}
				// write complete
				if (writeResult != null) {
					this.currentMessage.set(writeQueue.poll());
					handler.onMessageSent(this, currentMessage.getMessage());
					// try to get next message
					if (this.currentMessage.get() == null) {
						if (isLockedByMe) {
							isLockedByMe = false;
							writeLock.unlock();
						}
						// get next message
						WriteMessage nextMessage = writeQueue.peek();
						if (nextMessage != null && writeLock.tryLock()) {
							isLockedByMe = true;
							if (!writeQueue.isEmpty()
									&& this.currentMessage.compareAndSet(null,
											nextMessage)) {
								writeQueue.remove();
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
						writeLock.unlock();
					}
					// register OP_WRITE event
					selectorManager.registerSession(this,
							EventType.ENABLE_WRITE);
					break;
				}
			}
		} catch (IOException e) {
			handler.onExceptionCaught(this, e);
			if (currentMessage != null
					&& currentMessage.getWriteFuture() != null) {
				currentMessage.getWriteFuture().failure(e);
			}
			if (isLockedByMe) {
				isLockedByMe = false;
				writeLock.unlock();
			}
			close();
		} finally {
			if (isLockedByMe) {
				writeLock.unlock();
			}
		}
	}

	public final void enableWrite(Selector selector) {
		SelectionKey key = selectableChannel.keyFor(selector);
		if (key != null && key.isValid()) {
			interestWrite(key);
		} else {
			try {
				selectableChannel.register(selector,
						SelectionKey.OP_WRITE, this);
			} catch (ClosedChannelException e) {
				// ignore
			} catch (CancelledKeyException e) {
				// ignore
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
		writeLock.lock();
		try {
			if (getAttribute(SelectorManager.REACTOR_ATTRIBUTE) != null) {
				((Reactor) getAttribute(SelectorManager.REACTOR_ATTRIBUTE))
						.unregisterChannel(selectableChannel);
			}
			if (selectableChannel.isOpen()) {
				selectableChannel.close();
			}
		} finally {
			writeLock.unlock();
		}
	}

	protected final void registerSession() {
		selectorManager.registerSession(this, EventType.REGISTER);
	}

	protected void unregisterSession() {
		selectorManager.registerSession(this, EventType.UNREGISTER);
	}

	@Override
	public void writeFromUserCode(WriteMessage message) {
		if (schduleWriteMessage(message)) {
			return;
		}
		// 到这里，当前线程一定是IO线程
		onWrite(null);

	}

	protected boolean schduleWriteMessage(WriteMessage writeMessage) {
		boolean offered = writeQueue.offer(writeMessage);
		assert offered;
		final Reactor reactor = selectorManager.getReactorFromSession(this);
		if (Thread.currentThread() != reactor) {
			selectorManager.registerSession(this, EventType.ENABLE_WRITE);
			return true;
		}
		return false;
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
					tmpKey = selectableChannel.register(writeSelector,
							SelectionKey.OP_WRITE);
				}
				if (writeSelector.select(1000) == 0) {
					attempts++;
					if (attempts > 2) {
						return;
					}
				} else {
					break;
				}
			}
			onWrite(selectableChannel.keyFor(writeSelector));
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
		SelectionKey key = selectableChannel.keyFor(selector);

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
