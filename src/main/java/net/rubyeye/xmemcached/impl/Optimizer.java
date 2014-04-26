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
package net.rubyeye.xmemcached.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.MemcachedOptimizer;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.AssocCommandAware;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.command.binary.BaseBinaryCommand;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.BinaryGetMultiCommand;
import net.rubyeye.xmemcached.command.binary.BinarySetMultiCommand;
import net.rubyeye.xmemcached.command.binary.BinaryStoreCommand;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.monitor.MemcachedClientNameHolder;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.OpaqueGenerater;
import net.rubyeye.xmemcached.utils.Protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.impl.FutureImpl;

/**
 * Memcached command optimizer,merge single-get comands to multi-get
 * command,merge ByteBuffers to fit the socket's sendBufferSize etc.
 * 
 * @author dennis
 */
public class Optimizer implements OptimizerMBean, MemcachedOptimizer {

	public static final int DEFAULT_MERGE_FACTOR = 50;
	private int mergeFactor = DEFAULT_MERGE_FACTOR; // default merge factor;
	private boolean optimiezeGet = true;
	private final boolean optimiezeSet = true;
	private boolean optimiezeMergeBuffer = true;
	private static final Logger log = LoggerFactory.getLogger(Optimizer.class);
	private Protocol protocol = Protocol.Binary;

	public Optimizer(Protocol protocol) {
		XMemcachedMbeanServer.getInstance().registMBean(
				this,
				this.getClass().getPackage().getName() + ":type="
						+ this.getClass().getSimpleName() + "-"
						+ MemcachedClientNameHolder.getName());
		this.protocol = protocol;
	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {

	}

	public int getMergeFactor() {
		return this.mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		if (this.mergeFactor != mergeFactor) {
			log.warn("change mergeFactor from " + this.mergeFactor + " to "
					+ mergeFactor);
		}
		this.mergeFactor = mergeFactor;

	}

	public boolean isOptimizeGet() {
		return this.optimiezeGet;
	}

	public void setOptimizeGet(boolean optimiezeGet) {
		log.warn(optimiezeGet ? "Enable merge get commands"
				: "Disable merge get commands");
		this.optimiezeGet = optimiezeGet;
	}

	public boolean isOptimizeMergeBuffer() {
		return this.optimiezeMergeBuffer;
	}

	public void setOptimizeMergeBuffer(boolean optimiezeMergeBuffer) {
		log.warn(optimiezeMergeBuffer ? "Enable merge buffers"
				: "Disable merge buffers");
		this.optimiezeMergeBuffer = optimiezeMergeBuffer;
	}

	@SuppressWarnings("unchecked")
	public Command optimize(final Command currentCommand,
			final Queue writeQueue, final Queue<Command> executingCmds,
			int sendBufferSize) {
		Command optimiezeCommand = currentCommand;
		optimiezeCommand = this.optimiezeGet(writeQueue, executingCmds,
				optimiezeCommand);
		optimiezeCommand = this.optimiezeSet(writeQueue, executingCmds,
				optimiezeCommand, sendBufferSize);
		optimiezeCommand = this.optimiezeMergeBuffer(optimiezeCommand, writeQueue,
				executingCmds, sendBufferSize);
		return optimiezeCommand;
	}

	/**
	 * merge buffers to fit socket's send buffer size
	 * 
	 * @param currentCommand
	 * @return
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public final Command optimiezeMergeBuffer(Command optimiezeCommand,
			final Queue writeQueue, final Queue<Command> executingCmds,
			int sendBufferSize) {
		if (log.isDebugEnabled()) {
			log.debug("Optimieze merge buffer:" + optimiezeCommand.toString());
		}
		if (this.optimiezeMergeBuffer
				&& optimiezeCommand.getIoBuffer().remaining() < sendBufferSize) {
			optimiezeCommand = this.mergeBuffer(optimiezeCommand, writeQueue,
					executingCmds, sendBufferSize);
		}
		return optimiezeCommand;
	}

	/**
	 * Merge get operation to multi-get operation
	 * 
	 * @param currentCmd
	 * @param mergeCommands
	 * @return
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public final Command optimiezeGet(final Queue writeQueue,
			final Queue<Command> executingCmds, Command optimiezeCommand) {
		if (optimiezeCommand.getCommandType() == CommandType.GET_ONE
				|| optimiezeCommand.getCommandType() == CommandType.GETS_ONE) {
			if (this.optimiezeGet) {
				optimiezeCommand = this.mergeGetCommands(optimiezeCommand,
						writeQueue, executingCmds,
						optimiezeCommand.getCommandType());
			}
		}
		return optimiezeCommand;
	}

	public final Command optimiezeSet(final Queue writeQueue,
			final Queue<Command> executingCmds, Command optimiezeCommand,
			int sendBufferSize) {
		if (this.optimiezeSet
				&& optimiezeCommand.getCommandType() == CommandType.SET
				&& !optimiezeCommand.isNoreply() && this.protocol == Protocol.Binary) {
			optimiezeCommand = this.mergeSetCommands(optimiezeCommand, writeQueue,
					executingCmds, optimiezeCommand.getCommandType(),
					sendBufferSize);
		}
		return optimiezeCommand;
	}

	@SuppressWarnings("unchecked")
	private final Command mergeBuffer(final Command firstCommand,
			final Queue writeQueue, final Queue<Command> executingCmds,
			final int sendBufferSize) {
		Command lastCommand = firstCommand;
		Command nextCmd = (Command) writeQueue.peek();
		if (nextCmd == null) {
			return lastCommand;
		}

		final List<Command> commands = this.getLocalList();
		final ByteBuffer firstBuffer = firstCommand.getIoBuffer().buf();
		int totalBytes = firstBuffer.remaining();
		commands.add(firstCommand);
		boolean wasFirst = true;
		while (totalBytes + nextCmd.getIoBuffer().remaining() <= sendBufferSize
				&& (nextCmd = (Command) writeQueue.peek()) != null) {
			if (nextCmd.getStatus() == OperationStatus.WRITING) {
				break;
			}
			if (nextCmd.isCancel()) {
				writeQueue.remove();
				continue;
			}
			nextCmd.setStatus(OperationStatus.WRITING);

			writeQueue.remove();

			if (wasFirst) {
				wasFirst = false;
			}
			// if it is get_one command,try to merge get commands
			if ((nextCmd.getCommandType() == CommandType.GET_ONE || nextCmd
					.getCommandType() == CommandType.GETS_ONE) && this.optimiezeGet) {
				nextCmd = this.mergeGetCommands(nextCmd, writeQueue, executingCmds,
						nextCmd.getCommandType());
			}

			commands.add(nextCmd);
			lastCommand = nextCmd;
			totalBytes += nextCmd.getIoBuffer().remaining();
			if (totalBytes > sendBufferSize) {
				break;
			}

		}
		if (commands.size() > 1) {
			byte[] buf = new byte[totalBytes];
			int offset = 0;
			for (Command command : commands) {
				byte[] ba = command.getIoBuffer().array();
				System.arraycopy(ba, 0, buf, offset, ba.length);
				offset += ba.length;
				if (command != lastCommand
						&& (!command.isNoreply() || command instanceof BaseBinaryCommand)) {
					executingCmds.add(command);
				}
			}
			lastCommand.setIoBuffer(IoBuffer.wrap(buf));
		}
		return lastCommand;
	}

	private final ThreadLocal<List<Command>> threadLocal = new ThreadLocal<List<Command>>() {

		@Override
		protected List<Command> initialValue() {
			return new ArrayList<Command>(Optimizer.this.mergeFactor);
		}
	};

	public final List<Command> getLocalList() {
		List<Command> list = this.threadLocal.get();
		list.clear();
		return list;
	}

	static interface CommandCollector {
		public Object getResult();

		public void visit(Command command);

		public void finish();
		
		public CommandCollector reset();
	}

	static class KeyStringCollector implements CommandCollector {
		char[] buf = new char[1024*2];
		int count = 0;
		boolean wasFirst = true;

		public CommandCollector reset() {
			this.count = 0;
			this.wasFirst = true;
			return this;
		}

		public Object getResult() {
			return new String(this.buf, 0, this.count);
		}

		public void visit(Command command) {
			if (this.wasFirst) {
				this.append(command.getKey());
				this.wasFirst = false;
			} else {
				this.append(" ");
				this.append(command.getKey());
			}
		}

		private void expandCapacity(int minimumCapacity) {
			int newCapacity = (this.buf.length + 1) * 2;
			if (newCapacity < 0) {
				newCapacity = Integer.MAX_VALUE;
			} else if (minimumCapacity > newCapacity) {
				newCapacity = minimumCapacity;
			}
			char[] copy = new char[newCapacity];
			System.arraycopy(this.buf, 0, copy, 0, Math.min(this.buf.length, newCapacity));
			this.buf = copy;
		}

		private void append(String str) {
			int len = str.length();
			if (len == 0) {
				return;
			}
			int newCount = this.count + len;
			if (newCount > this.buf.length) {
				this.expandCapacity(newCount);
			}
			str.getChars(0, len, this.buf, this.count);
			this.count = newCount;
		}

		public void finish() {
			// do nothing

		}

	}

	private static class BinarySetQCollector implements CommandCollector {
		ArrayList<IoBuffer> bufferList = new ArrayList<IoBuffer>();
		int totalBytes;
		BinaryStoreCommand prevCommand;
		Map<Object, Command> mergeCommands;

		
		
		public CommandCollector reset() {
			this.bufferList.clear();
			this.totalBytes = 0;
			this.prevCommand = null;
			this.mergeCommands = null;
			return this;
		}

		public Object getResult() {
			byte[] buf = new byte[this.totalBytes];
			int offset = 0;
			for (IoBuffer buffer : this.bufferList) {
				byte[] ba = buffer.array();
				System.arraycopy(ba, 0, buf, offset, ba.length);
				offset += ba.length;
			}
			BinarySetMultiCommand resultCommand = new BinarySetMultiCommand(
					null, CommandType.SET_MANY, new CountDownLatch(1));
			resultCommand.setIoBuffer(IoBuffer.wrap(buf));
			resultCommand.setMergeCommands(this.mergeCommands);
			resultCommand.setMergeCount(this.mergeCommands.size());
			return resultCommand;
		}

		public void visit(Command command) {

			// Encode prev command
			if (this.prevCommand != null) {
				// first n-1 send setq command
				BinaryStoreCommand setqCmd = new BinaryStoreCommand(
						this.prevCommand.getKey(), this.prevCommand.getKeyBytes(),
						CommandType.SET, null, this.prevCommand.getExpTime(),
						this.prevCommand.getCas(),
						// set noreply to be true
						this.prevCommand.getValue(), true,
						this.prevCommand.getTranscoder());
				// We must set the opaque to get error message.
				int opaque = OpaqueGenerater.getInstance().getNextValue();
				setqCmd.setOpaque(opaque);
				setqCmd.encode();
				this.totalBytes += setqCmd.getIoBuffer().remaining();


				this.bufferList.add(setqCmd.getIoBuffer());
				// GC friendly
				setqCmd.setIoBuffer(MemcachedHandler.EMPTY_BUF);
				setqCmd.setValue(null);
				this.prevCommand.setValue(null);
				this.prevCommand.setIoBuffer(MemcachedHandler.EMPTY_BUF);
				if (this.mergeCommands == null) {
					this.mergeCommands = new HashMap<Object, Command>();
				}
				this.mergeCommands.put(opaque, this.prevCommand);
			}
			this.prevCommand = (BinaryStoreCommand) command;
		}

		public void finish() {
			if (this.mergeCommands == null) {
				return;
			}
			// prevCommand is the last command,last command must be a SET
			// command,ensure
			// previous SETQ commands sending response back
			BinaryStoreCommand setqCmd = new BinaryStoreCommand(
					this.prevCommand.getKey(), this.prevCommand.getKeyBytes(),
					CommandType.SET, null, this.prevCommand.getExpTime(),
					this.prevCommand.getCas(),
					// set noreply to be false.
					this.prevCommand.getValue(), false, this.prevCommand.getTranscoder());
			// We must set the opaque to get error message.
			int opaque = OpaqueGenerater.getInstance().getNextValue();
			setqCmd.setOpaque(opaque);
			setqCmd.encode();
			this.bufferList.add(setqCmd.getIoBuffer());
			this.totalBytes += setqCmd.getIoBuffer().remaining();
			if (this.mergeCommands != null) {
				this.mergeCommands.put(opaque, this.prevCommand);
			}
		}

	}

	private static class BinaryGetQCollector implements CommandCollector {
		ArrayList<IoBuffer> bufferList = new ArrayList<IoBuffer>(50);
		int totalBytes;
		Command prevCommand;
		
		public CommandCollector reset(){
			this.bufferList.clear();
			this.totalBytes = 0;
			this.prevCommand = null;
			return this;
		}

		public Object getResult() {
			byte[] buf = new byte[this.totalBytes];
			int offset = 0;
			for (IoBuffer buffer : this.bufferList) {
				byte[] ba = buffer.array();
				System.arraycopy(ba, 0, buf, offset, ba.length);
				offset += ba.length;
			}
			BinaryGetMultiCommand resultCommand = new BinaryGetMultiCommand(
					null, CommandType.GET_MANY, new CountDownLatch(1));
			resultCommand.setIoBuffer(IoBuffer.wrap(buf));
			return resultCommand;
		}

		public void visit(Command command) {
			// Encode prev command
			if (this.prevCommand != null) {
				// first n-1 send getq command
				Command getqCommand = new BinaryGetCommand(
						this.prevCommand.getKey(), this.prevCommand.getKeyBytes(), null,
						null, OpCode.GET_KEY_QUIETLY, true);
				getqCommand.encode();
				this.totalBytes += getqCommand.getIoBuffer().remaining();
				this.bufferList.add(getqCommand.getIoBuffer());
			}
			this.prevCommand = command;
		}

		public void finish() {
			// prev command is the last command,last command must be getk,ensure
			// getq commands sending response back
			Command lastGetKCommand = new BinaryGetCommand(
					this.prevCommand.getKey(), this.prevCommand.getKeyBytes(),
					CommandType.GET_ONE, new CountDownLatch(1), OpCode.GET_KEY,
					false);
			lastGetKCommand.encode();
			this.bufferList.add(lastGetKCommand.getIoBuffer());
			this.totalBytes += lastGetKCommand.getIoBuffer().remaining();
		}

	}

	@SuppressWarnings("unchecked")
	private final Command mergeGetCommands(final Command currentCmd,
			final Queue writeQueue, final Queue<Command> executingCmds,
			CommandType expectedCommandType) {
		Map<Object, Command> mergeCommands = null;
		int mergeCount = 1;
		final CommandCollector commandCollector = this.createGetCommandCollector();
		currentCmd.setStatus(OperationStatus.WRITING);

		commandCollector.visit(currentCmd);
		while (mergeCount < this.mergeFactor) {
			Command nextCmd = (Command) writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
			if (nextCmd.isCancel()) {
				writeQueue.remove();
				continue;
			}
			if (nextCmd.getCommandType() == expectedCommandType) {
				if (mergeCommands == null) { // lazy initialize
					mergeCommands = new HashMap<Object, Command>(
							this.mergeFactor / 2);
					mergeCommands.put(currentCmd.getKey(), currentCmd);
				}
				if (log.isDebugEnabled()) {
					log.debug("Merge get command:" + nextCmd.toString());
				}
				nextCmd.setStatus(OperationStatus.WRITING);
				Command removedCommand = (Command) writeQueue.remove();
				// If the key is exists,add the command to associated list.
				if (mergeCommands.containsKey(removedCommand.getKey())) {
					final AssocCommandAware mergedGetCommand = (AssocCommandAware) mergeCommands
							.get(removedCommand.getKey());
					if (mergedGetCommand.getAssocCommands() == null) {
						mergedGetCommand
						.setAssocCommands(new ArrayList<Command>(5));
					}
					mergedGetCommand.getAssocCommands().add(removedCommand);
				} else {
					commandCollector.visit(nextCmd);
					mergeCommands.put(removedCommand.getKey(), removedCommand);
				}
				mergeCount++;
			} else {
				break;
			}
		}
		if (mergeCount == 1) {
			return currentCmd;
		} else {
			commandCollector.finish();
			if (log.isDebugEnabled()) {
				log.debug("Merge optimieze:merge " + mergeCount
						+ " get commands");
			}
			return this.newMergedCommand(mergeCommands, mergeCount,
					commandCollector, expectedCommandType);
		}
	}
	
	private static final ThreadLocal<BinarySetQCollector> BIN_SET_CMD_COLLECTOR_THREAD_LOCAL = new ThreadLocal<Optimizer.BinarySetQCollector>(){

		@Override
		protected BinarySetQCollector initialValue() {
			return new BinarySetQCollector();
		}
		
	};

	private final Command mergeSetCommands(final Command currentCmd,
			final Queue writeQueue, final Queue<Command> executingCmds,
			CommandType expectedCommandType, int sendBufferSize) {
		int mergeCount = 1;
		final CommandCollector commandCollector = BIN_SET_CMD_COLLECTOR_THREAD_LOCAL.get().reset();
		currentCmd.setStatus(OperationStatus.WRITING);
		int totalBytes = currentCmd.getIoBuffer().remaining();
		commandCollector.visit(currentCmd);
		while (mergeCount < this.mergeFactor && totalBytes <= sendBufferSize) {
			Command nextCmd = (Command) writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
			if (nextCmd.isCancel()) {
				writeQueue.remove();
				continue;
			}
			if (nextCmd.getCommandType() == expectedCommandType
					&& !nextCmd.isNoreply()) {
				if (log.isDebugEnabled()) {
					log.debug("Merge set command:" + nextCmd.toString());
				}
				nextCmd.setStatus(OperationStatus.WRITING);
				writeQueue.remove();

				commandCollector.visit(nextCmd);

				mergeCount++;
			} else {
				break;
			}
			totalBytes += nextCmd.getIoBuffer().remaining();
		}
		if (mergeCount == 1) {
			return currentCmd;
		} else {
			commandCollector.finish();
			if (log.isDebugEnabled()) {
				log.debug("Merge optimieze:merge " + mergeCount
						+ " get commands");
			}
			return (Command) commandCollector.getResult();
		}
	}

	private static ThreadLocal<KeyStringCollector> TEXT_GET_CMD_COLLECTOR_THREAD_LOCAL = new ThreadLocal<Optimizer.KeyStringCollector>(){
		@Override
		public KeyStringCollector initialValue(){
			return new KeyStringCollector();
		}
	};
	
	private static ThreadLocal<BinaryGetQCollector> BIN_GET_CMD_COLLECTOR_THREAD_LOCAL = new ThreadLocal<Optimizer.BinaryGetQCollector>(){
		@Override
		public BinaryGetQCollector initialValue(){
			return new BinaryGetQCollector();
		}
	};
	
	private CommandCollector createGetCommandCollector() {
		switch (this.protocol) {
		case Binary:
			return BIN_GET_CMD_COLLECTOR_THREAD_LOCAL.get().reset();
		default:
			return TEXT_GET_CMD_COLLECTOR_THREAD_LOCAL.get().reset();
		}
	}

	private Command newMergedCommand(final Map<Object, Command> mergeCommands,
			int mergeCount, final CommandCollector commandCollector,
			final CommandType commandType) {
		if (this.protocol == Protocol.Text) {
			String resultKey = (String) commandCollector.getResult();

			byte[] keyBytes = ByteUtils.getBytes(resultKey);
			byte[] cmdBytes = commandType == CommandType.GET_ONE ? Constants.GET
					: Constants.GETS;
			final byte[] buf = new byte[cmdBytes.length + 3 + keyBytes.length];
			ByteUtils.setArguments(buf, 0, cmdBytes, keyBytes);
			TextGetOneCommand cmd = new TextGetOneCommand(resultKey, keyBytes,
					commandType, null);
			cmd.setMergeCommands(mergeCommands);
			cmd.setWriteFuture(new FutureImpl<Boolean>());
			cmd.setMergeCount(mergeCount);
			cmd.setIoBuffer(IoBuffer.wrap(buf));
			return cmd;
		} else {
			BinaryGetMultiCommand result = (BinaryGetMultiCommand) commandCollector
					.getResult();
			result.setMergeCount(mergeCount);
			result.setMergeCommands(mergeCommands);
			return result;
		}
	}
}
