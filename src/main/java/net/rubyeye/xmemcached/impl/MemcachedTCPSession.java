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
package net.rubyeye.xmemcached.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.Future;

import net.rubyeye.xmemcached.MemcachedOptimizer;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.OperationStatus;

import com.google.code.yanf4j.nio.WriteMessage;
import com.google.code.yanf4j.nio.impl.DefaultTCPSession;
import com.google.code.yanf4j.nio.impl.SessionConfig;
import com.google.code.yanf4j.nio.util.FutureImpl;
import com.google.code.yanf4j.util.LinkedTransferQueue;

/**
 * Connected session for a memcached server
 * 
 * @author dennis
 */
public class MemcachedTCPSession extends DefaultTCPSession {

	/**
	 * Command which are already sent
	 */
	protected Queue<Command> executingCmds;

	private volatile int weight;

	private final Object empty = new Object();

	private SocketAddress remoteSocketAddress; // prevent channel is closed
	private int sendBufferSize;
	private MemcachedOptimizer optimiezer;
	private volatile boolean allowReconnect;

	public static final String CURRENT_GET_KEY = "current_key";

	public static final String CURRENT_GET_VALUES = "current_values";

	public static final String CURRENT_LINE_ATTR = "current_line";

	public static final String PARSE_STATUS_ATTR = "parse_status";

	public final int getWeight() {
		return this.weight;
	}

	public final void setWeight(int weight) {
		this.weight = weight;
	}

	public MemcachedTCPSession(SessionConfig sessionConfig,
			int readRecvBufferSize, MemcachedOptimizer optimiezer,
			int readThreadCount, int weight) {
		super(sessionConfig, readRecvBufferSize, -1);
		this.optimiezer = optimiezer;
		this.weight = weight;
		if (this.selectableChannel != null) {
			this.remoteSocketAddress = ((SocketChannel) this.selectableChannel)
					.socket().getRemoteSocketAddress();
			this.allowReconnect = true;
			try {
				this.sendBufferSize = ((SocketChannel) this.selectableChannel)
						.socket().getSendBufferSize();
			} catch (SocketException e) {
				this.sendBufferSize = 8 * 1024;
			}
		}
		this.executingCmds = new LinkedTransferQueue<Command>();
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		InetSocketAddress result = super.getRemoteSocketAddress();
		if (result == null) {
			result = (InetSocketAddress) this.remoteSocketAddress;
		}
		return result;
	}

	@Override
	protected WriteMessage preprocessWriteMessage(WriteMessage writeMessage) {
		Command currentCommand = (Command) writeMessage;
		// Check if IoBuffer is null
		if (currentCommand.getIoBuffer() == null) {
			currentCommand.encode(this.bufferAllocator);
		}
		// Check if it is canceled.
		if (currentCommand.isCancel()) {
			this.writeQueue.remove();
			return null;
		}
		if (currentCommand.getStatus() == OperationStatus.SENDING) {
			/**
			 * optimieze commands
			 */
			currentCommand = this.optimiezer.optimize(currentCommand,
					this.writeQueue, this.executingCmds, this.sendBufferSize);
		}
		currentCommand.setStatus(OperationStatus.WRITING);
		return currentCommand;
	}

	private BufferAllocator bufferAllocator;

	public final BufferAllocator getBufferAllocator() {
		return this.bufferAllocator;
	}

	public final void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	@Override
	protected final WriteMessage wrapMessage(Object msg,
			Future<Boolean> writeFuture) {
		((Command) msg).encode(this.bufferAllocator);
		((Command) msg).setWriteFuture((FutureImpl<Boolean>) writeFuture);
		if (log.isDebugEnabled()) {
			log.debug("After encoding" + ((Command) msg).toString());
		}
		return (WriteMessage) msg;
	}

	/**
	 * get current command from queue
	 * 
	 * @return
	 */
	public final Command pollCurrentExecutingCommand() {
		return this.executingCmds.poll();
	}

	/**
	 * peek current command from queue
	 * 
	 * @return
	 */
	public final Command peekCurrentExecutingCommand() {
		try {
			synchronized (this.empty) {
				while (this.executingCmds.peek() == null) {
					this.empty.wait(1000);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return this.executingCmds.peek();
	}

	/**
	 * is allow auto recconect if closed?
	 * 
	 * @return
	 */
	public boolean isAllowReconnect() {
		return this.allowReconnect;
	}

	public void setAllowReconnect(boolean reconnected) {
		this.allowReconnect = reconnected;
	}

	public final void addCommand(Command command) {
		this.executingCmds.add(command);
		synchronized (this.empty) {
			this.empty.notifyAll();
		}
	}
}
