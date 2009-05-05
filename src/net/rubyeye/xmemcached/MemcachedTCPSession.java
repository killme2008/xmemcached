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

import com.google.code.yanf4j.nio.impl.DefaultTCPSession;
import com.google.code.yanf4j.nio.impl.SessionConfig;
import com.google.code.yanf4j.nio.util.EventType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.rubyeye.xmemcached.MemcachedHandler.ParseStatus;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.OperationStatus;

import net.rubyeye.xmemcached.utils.SimpleBlockingQueue;
import net.spy.memcached.transcoders.CachedData;

/**
 * 针对memcached的session类
 * 
 * @author dennis
 */
public class MemcachedTCPSession extends DefaultTCPSession {

	String currentLine = null; // 当前行
	ParseStatus status = ParseStatus.NULL; // 当前状态
	private MemcachedProtocolHandler memcachedProtocolHandler;
	protected BlockingQueue<Command> executingCmds; // 存储已经发送的命令
	Map<String, CachedData> currentValues = null;
	private SocketAddress remoteSocketAddress;
	private int sendBufferSize;
	private MemcachedOptimiezer optimiezer;

	public MemcachedTCPSession(SessionConfig sessionConfig,
			int readRecvBufferSize, MemcachedOptimiezer optimiezer,
			int readThreadCount) {
		super(sessionConfig, readRecvBufferSize, -1);
		this.optimiezer = optimiezer;
		remoteSocketAddress = ((SocketChannel) this.selectableChannel).socket()
				.getRemoteSocketAddress();
		try {
			this.sendBufferSize = ((SocketChannel) this.selectableChannel)
					.socket().getSendBufferSize();
		} catch (SocketException e) {
			this.sendBufferSize = 8 * 1024;
		}
		if (readThreadCount > 0) {
			this.executingCmds = new ArrayBlockingQueue<Command>(16 * 1024);
		} else {
			this.executingCmds = new SimpleBlockingQueue<Command>();
		}
	}

	public InetSocketAddress getRemoteSocketAddress() {
		return (InetSocketAddress) remoteSocketAddress;
	}

	public void setMemcachedProtocolHandler(
			MemcachedProtocolHandler memcachedProtocolHandler) {
		this.memcachedProtocolHandler = memcachedProtocolHandler;
	}

	public MemcachedProtocolHandler getMemcachedProtocolHandler() {
		return this.memcachedProtocolHandler;
	}

	@SuppressWarnings("unchecked")
	protected void onWrite() {
		Command currentCommand = null;
		try {
			if (getSessionStatus() == SessionStatus.WRITING) // 用户可能正在调用flush方法
			{
				return;
			}
			if (getSessionStatus() == SessionStatus.READING // 不允许读写并行
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
				// 判断是否已经被取消
				if (currentCommand.isCancel()) {
					writeQueue.remove();
					continue;
				}
				if (currentCommand.getStatus() == OperationStatus.SENDING) {
					currentCommand = this.optimiezer.optimieze(currentCommand,
							this.writeQueue, this.executingCmds,
							this.sendBufferSize);
				}
				currentCommand.setStatus(OperationStatus.WRITING);
				boolean complete = writeToChannel(selectableChannel,
						currentCommand.getIoBuffer().getByteBuffer());
				if (complete) {
					if (log.isDebugEnabled()) {

						log.debug("send command:" + currentCommand.toString());
					}
					this.handler.onMessageSent(this, writeQueue.remove());
				} else { // not write complete, but write buffer is full
					break;
				}
			}
			if (!writeComplete) {
				sessionEventManager.registerSession(this,
						EventType.ENABLE_WRITE);
			}
			setSessionStatus(SessionStatus.IDLE);
		} catch (CancelledKeyException cke) {
			log.error(cke, cke);
			onException(cke);
			close();

		} catch (ClosedChannelException cce) {
			log.error(cce, cce);
			onException(cce);
			close();
		} catch (IOException ioe) {
			log.error(ioe, ioe);
			onException(ioe);
			close();
		} catch (Exception e) {
			onException(e);
			log.error(e, e);
			close();
		}
	}

	protected boolean writeToChannel(SelectableChannel channel,
			ByteBuffer writeBuffer) throws IOException {
		while (true) {
			if (writeBuffer == null || !writeBuffer.hasRemaining())
				return true;
			long n = doRealWrite(channel, writeBuffer);
			if (writeBuffer == null || !writeBuffer.hasRemaining()) {
				return true;
			} else if (n == 0) {
				return false; // 未写完，等待下次写
			}
		}
	}

	protected long doRealWrite(SelectableChannel channel, ByteBuffer buffer)
			throws IOException {
		return ((WritableByteChannel) (channel)).write(buffer);
	}

	/**
	 * 解码，产生message，调用处理器处理
	 */
	public void decode() {
		while (readBuffer.hasRemaining()) {
			try {
				// 使用MemcachedProtocolHandler解析协议
				if (!this.memcachedProtocolHandler.onReceive(this, readBuffer)) {
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

	public final void resetStatus() {
		status = ParseStatus.NULL;
		currentLine = null;
	}

	/**
	 * 获取当前执行command
	 * 
	 * @return
	 */
	Command pollCurrentExecutingCommand() {
		try {
			Command cmd = executingCmds.take();
			if (cmd != null) {
				cmd.setStatus(OperationStatus.PROCESSING);
			} else {
				throw new NoSuchElementException();
			}
			return cmd;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	Command peekCurrentExecutingCommand() {
		Command cmd = executingCmds.peek();
		if (cmd == null)
			throw new NoSuchElementException();
		return cmd;
	}

	@Override
	protected final Object wrapMessage(Object msg) {
		return msg;
	}

}
