/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rubyeye.xmemcached;

import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.impl.DefaultTCPSession;
import com.google.code.yanf4j.nio.impl.SessionEventManager;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.nio.util.SelectorFactory;
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.util.Queue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.MemcachedHandler.ParseStatus;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.ExtendedQueue;
import net.spy.memcached.transcoders.CachedData;

/**
 * 针对memcached的session类
 * 
 * @author dennis
 */
public class MemcachedTCPSession extends DefaultTCPSession {

	String currentLine = null; // 当前行

	/**
	 * 协议解析部分
	 */
	ParseStatus status = ParseStatus.NULL; // 当前状态

	private MemcachedProtocolHandler memcachedProtocolHandler;
	protected List<Command> executingCmds = new LinkedList<Command>(); // 存储已经发送的命令

	Map<String,CachedData> currentValues=null;

	private SocketAddress remoteSocketAddress;

	@SuppressWarnings("unchecked")
	public MemcachedTCPSession(SocketChannel sc, SelectionKey sk,
			Handler handler, SessionEventManager reactor,
			CodecFactory codecFactory, int readRecvBufferSize,
			Statistics statistics, Queue<WriteMessage> queue,
			long sessionTimeout, boolean handleReadWriteConcurrently,
			boolean optimiezeGet, boolean optimiezeSet) {
		super(sc, sk, handler, reactor, codecFactory, readRecvBufferSize,
				statistics, queue, sessionTimeout, handleReadWriteConcurrently);
		this.optimiezeGet = optimiezeGet;
		this.optimiezeSet = optimiezeSet;
		remoteSocketAddress = sc.socket().getRemoteSocketAddress();
	}

	public InetSocketAddress getRemoteSocketAddress() {
		return (InetSocketAddress) remoteSocketAddress;
	}

	public void setMergeGetsCount(int n) {
		this.getsMergeFactor = n;
	}

	public void setMemcachedProtocolHandler(
			MemcachedProtocolHandler memcachedProtocolHandler) {
		this.memcachedProtocolHandler = memcachedProtocolHandler;
	}

	public MemcachedProtocolHandler getMemcachedProtocolHandler() {
		return this.memcachedProtocolHandler;
	}

	@SuppressWarnings("unchecked")
	public boolean send(Object msg) throws InterruptedException {
		if (isClose()) {
			return false;
		}
		Command message = (Command) msg;
		writeQueue.getLock().lock();
		try {
			if (writeQueue.isEmpty()) {
				if (writeQueue.push(message)) {
					sessionEventManager.registerSession(this,
							EventType.ENABLE_WRITE); // 列表为空，注册监听写事件

					return true;
				} else {
					return false;
				}
			} else {
				return writeQueue.push(message);
			}
		} finally {
			writeQueue.getLock().unlock();
			selectionKey.selector().wakeup();
		}
	}

	/**
	 * 优化set操作，连续的set如果key相同将合并成最后一个set操作
	 * 
	 * @param currentCmd
	 * @return
	 * @throws InterruptedException
	 */
	private Command optimizeSet(Command currentCmd) throws InterruptedException {
		Command finalCommand = currentCmd;
		while (true) {
			Command nextCmd = (Command) this.writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
			if (!nextCmd.getKey().equals(currentCmd.getKey())) {
				break;
			}
			Command.CommandType nextCommandType = nextCmd.getCommandType();
			// 相同key，后续的set将覆盖此时的set
			if (nextCommandType.equals(Command.CommandType.SET)) {
				finalCommand.getLatch().countDown();
				finalCommand = (Command) writeQueue.pop();
			} else {
				break;
			}
		}
		return finalCommand;
	}

	protected int getsMergeFactor = 60; // get操作合并参数

	protected boolean optimiezeGet = true; // 是否启用get优化

	protected boolean optimiezeSet = false; // 是否启用set优化

	/**
	 * 优化get操作，连续的get操作将合并成一个
	 * 
	 * @param currentCmd
	 * @param mergeCommands
	 * @return
	 * @throws InterruptedException
	 */
	private Command optimizeGet(Command currentCmd,
			final List<Command> mergeCommands) throws InterruptedException {
		int i = 1;
		final StringBuilder key = new StringBuilder();
		key.append((String) currentCmd.getKey());
		while (i < getsMergeFactor) {
			Command nextCmd = (Command) this.writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
			if (nextCmd.getCommandType().equals(Command.CommandType.GET_ONE)) {
				mergeCommands.add((Command) this.writeQueue.pop());
				key.append(" ").append((String) nextCmd.getKey());
				i++;
			} else {
				break;
			}
		}
		if (i == 1) {
			return currentCmd;
		} else {
			return newMergedCommand(mergeCommands, key);
		}
	}

	private Command newMergedCommand(final List<Command> mergeCommands,
			final StringBuilder key) {
		byte[] keyBytes = ByteUtils.getBytes(key.toString());
		final ByteBuffer buffer = ByteBuffer.allocate(ByteUtils.GET.length
				+ ByteUtils.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, ByteUtils.GET, keyBytes);
		buffer.flip();
		return new Command(key.toString(), Command.CommandType.GET_ONE, null) {

			public int getMergeCount() {
				return mergeCommands.size();
			}

			public List<Command> getMergeCommands() {
				return mergeCommands;
			}

			public ByteBuffer getByteBuffer() {
				setByteBuffer(buffer);
				return buffer;
			}
		};
	}

	@SuppressWarnings("unchecked")
	protected void onWrite() {
		Command currentCommand = null;
		try {
			if (getSessionStatus().equals(SessionStatus.WRITING)) // 用户可能正在调用flush方法
			{
				return;
			}
			if (getSessionStatus().equals(SessionStatus.READING) // 不允许读写并行
					&& !handleReadWriteConcurrently) {
				return;
			}
			selectionKey.interestOps(selectionKey.interestOps()
					& ~SelectionKey.OP_WRITE);
			setSessionStatus(SessionStatus.WRITING);
			boolean writeComplete = false;
			while (true) {
				currentCommand = (Command) writeQueue.peek();
				if (currentCommand == null) {
					writeComplete = true; // 写完队列

					break;
				}
				if (currentCommand.getCommandType().equals(
						Command.CommandType.GET_ONE)) {
					final List<Command> mergeCommands = new ArrayList<Command>(
							getsMergeFactor);
					mergeCommands.add(currentCommand);
					// 优化get操作
					if (optimiezeGet) {
						writeQueue.pop();
						currentCommand = optimizeGet(currentCommand,
								mergeCommands);
						((ExtendedQueue) writeQueue).addFirst(currentCommand); // 加入队首

					}
				} else if (currentCommand.getCommandType().equals(
						Command.CommandType.SET)
						&& optimiezeSet) {
					writeQueue.pop();
					currentCommand = optimizeSet(currentCommand);
					((ExtendedQueue) writeQueue).addFirst(currentCommand);// 加入队首

				}
				ByteBuffer buffer = writeToChannel(selectableChannel,
						currentCommand.getByteBuffer());
				if (buffer != null && !buffer.hasRemaining()) {
					writeQueue.pop(); // remove message

					executingCmds.add(currentCommand); // 添加到当前执行队列

				} else { // not write complete, but write buffer is full

					break;
				}
			}
			if (!writeComplete) {
				sessionEventManager.registerSession(this,
						EventType.ENABLE_WRITE); // listening
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
			log.error(ioe, ioe);
			handler.onException(this, ioe);
			close();
		} catch (Exception e) {
			handler.onException(this, e);
			log.error(e, e);
			close();
		}
	}

	@SuppressWarnings("unchecked")
	protected ByteBuffer writeToChannel(SelectableChannel channel,
			ByteBuffer writeBuffer) throws IOException {
		updateTimeStamp();
		if (!writeBuffer.hasRemaining()) {
			return writeBuffer; // Write completed

		}
		if (useBlockingWrite) { // 阻塞写

			return blockingWrite(channel, writeBuffer);
		} else {
			while (true) {
				long n = doRealWrite(channel, writeBuffer);
				if (!writeBuffer.hasRemaining()) {
					return writeBuffer;
				} else if (n == 0) {
					return null; // 未写完，等待下次写

				}
			}
		}

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
	protected ByteBuffer blockingWrite(SelectableChannel channel,
			ByteBuffer writeBuffer) throws IOException, ClosedChannelException {
		SelectionKey tmpKey = null;
		Selector writeSelector = null;
		int attempts = 0;
		int bytesProduced = 0;
		try {
			while (writeBuffer.hasRemaining()) {
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
						if (attempts > 2) {
							throw new IOException("Client disconnected");
						}
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
		return writeBuffer;
	}

	protected long doRealWrite(SelectableChannel channel, ByteBuffer buffer)
			throws IOException {
		return ((WritableByteChannel) (channel)).write(buffer);
	}

	/**
	 * 解码，产生message，调用处理器处理
	 */
	@SuppressWarnings("unchecked")
	public void decode() {
		boolean received = false;
		while (readBuffer.hasRemaining()) {
			try {
				// 使用MemcachedProtocolHandler解析协议
				received = this.memcachedProtocolHandler.onReceive(this,
						readBuffer);
				if (!received) {
					break;
				}
			} catch (Exception e) {
				handler.onException(this, e);
				log.error(e, e);
				e.printStackTrace();
				super.close();
				break;
			}
		}
	}

	public void resetStatus() {
		status = ParseStatus.NULL;
		currentLine = null;
	}

	/**
	 * 获取当前执行command
	 * 
	 * @return
	 */
	Command getCurrentExecutingCommand() {
		return executingCmds.remove(0);
	}
}
