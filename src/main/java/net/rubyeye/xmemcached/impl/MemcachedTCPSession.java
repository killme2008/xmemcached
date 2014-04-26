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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.MemcachedOptimizer;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.FutureImpl;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.nio.impl.NioTCPSession;
import com.google.code.yanf4j.util.LinkedTransferQueue;
import com.google.code.yanf4j.util.SystemUtils;

/**
 * Connected session for a memcached server
 * 
 * @author dennis
 */
public class MemcachedTCPSession extends NioTCPSession implements
		MemcachedSession {

	/**
	 * Command which are already sent
	 */
	protected BlockingQueue<Command> commandAlreadySent;

	private final AtomicReference<Command> currentCommand = new LinkedTransferQueue.PaddedAtomicReference<Command>(null);

	private SocketAddress remoteSocketAddress; // prevent channel is closed
	private int sendBufferSize;
	private final MemcachedOptimizer optimiezer;
	private volatile boolean allowReconnect;

	private volatile boolean authFailed;

	private final CommandFactory commandFactory;

	private InetSocketAddressWrapper inetSocketAddressWrapper;

	public MemcachedTCPSession(NioSessionConfig sessionConfig,
			int readRecvBufferSize, MemcachedOptimizer optimiezer,
			int readThreadCount, CommandFactory commandFactory) {
		super(sessionConfig, readRecvBufferSize);
		this.optimiezer = optimiezer;
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
		this.commandAlreadySent = (BlockingQueue<Command>)SystemUtils.createTransferQueue();
		this.commandFactory = commandFactory;
	}

	public InetSocketAddressWrapper getInetSocketAddressWrapper() {
		return this.inetSocketAddressWrapper;
	}

	public int getOrder() {
		return this.getInetSocketAddressWrapper().getOrder();
	}

	public int getWeight() {
		return this.getInetSocketAddressWrapper().getWeight();
	}

	public void setInetSocketAddressWrapper(
			InetSocketAddressWrapper inetSocketAddressWrapper) {
		this.inetSocketAddressWrapper = inetSocketAddressWrapper;
	}

	@Override
	public String toString() {
		return SystemUtils.getRawAddress(this.getRemoteSocketAddress()) + ":"
				+ this.getRemoteSocketAddress().getPort();
	}

	public void destroy() {
		Command command = this.currentCommand.get();	
		if (command != null) {
			command.setException(new MemcachedException(
					"Session has been closed"));
			CountDownLatch latch = command.getLatch();
			if (latch != null) {
				latch.countDown();
			}
		}
		while ((command = this.commandAlreadySent.poll()) != null) {
			command.setException(new MemcachedException(
					"Session has been closed"));
			CountDownLatch latch = command.getLatch();
			if (latch != null) {
				latch.countDown();
			}
		}

	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		InetSocketAddress result = super.getRemoteSocketAddress();
		if (result == null && this.remoteSocketAddress != null) {
			result = (InetSocketAddress) this.remoteSocketAddress;
		}
		return result;
	}

	@Override
	protected WriteMessage preprocessWriteMessage(WriteMessage writeMessage) {
		Command currentCommand = (Command) writeMessage;
		// Check if IoBuffer is null
		if (currentCommand.getIoBuffer() == null) {
			currentCommand.encode();
		}
		if (currentCommand.getStatus() == OperationStatus.SENDING) {
			/**
			 * optimieze commands
			 */
			currentCommand = this.optimiezer.optimize(currentCommand,
					this.writeQueue, this.commandAlreadySent,
					this.sendBufferSize);
		}
		currentCommand.setStatus(OperationStatus.WRITING);
		return currentCommand;
	}

	public boolean isAuthFailed() {
		return this.authFailed;
	}

	public void setAuthFailed(boolean authFailed) {
		this.authFailed = authFailed;
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
		((Command) msg).encode();
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
	private Command takeExecutingCommand() {
		try {
			return this.commandAlreadySent.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
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

	public void addCommand(Command command) {
		this.commandAlreadySent.add(command);
	}

	public void setCurrentCommand(Command cmd) {
		this.currentCommand.set(cmd);
	}

	public Command getCurrentCommand() {
		return this.currentCommand.get();
	}

	public void takeCurrentCommand() {
		this.setCurrentCommand(this.takeExecutingCommand());
	}

	public void quit() {
		this.write(this.commandFactory.createQuitCommand());
	}
}
