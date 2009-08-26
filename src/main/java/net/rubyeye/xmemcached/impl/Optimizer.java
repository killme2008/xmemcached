/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.command.AssocCommandAware;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.BinaryGetMultiCommand;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.Protocol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.nio.util.FutureImpl;

/**
 * Memcached command optimizer,merge single-get comands to multi-get
 * command,merge ByteBuffers to fit the socket's sendBufferSize etc.
 * 
 * @author dennis
 */
public class Optimizer implements OptimizerMBean, MemcachedOptimizer {

	public static final int DEFAULT_MERGE_FACTOR = 150;
	private int mergeFactor = DEFAULT_MERGE_FACTOR; // default merge factor;
	private boolean optimiezeGet = true;
	private boolean optimiezeMergeBuffer = true;
	private BufferAllocator bufferAllocator;
	private static final Log log = LogFactory.getLog(Optimizer.class);
	private Protocol protocol = Protocol.Binary;

	public Optimizer(Protocol protocol) {
		XMemcachedMbeanServer.getInstance().registMBean(
				this,
				this.getClass().getPackage().getName() + ":type="
						+ this.getClass().getSimpleName());
		this.protocol = protocol;
	}

	public BufferAllocator getBufferAllocator() {
		return this.bufferAllocator;
	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	public int getMergeFactor() {
		return this.mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		log.warn("change mergeFactor from " + this.mergeFactor + " to "
				+ mergeFactor);
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
		optimiezeCommand = optimiezeGet(writeQueue, executingCmds,
				optimiezeCommand);
		optimiezeCommand = optimiezeMergeBuffer(optimiezeCommand, writeQueue,
				executingCmds, sendBufferSize);
		return optimiezeCommand;
	}

	/**
	 *merge buffers to fit socket's send buffer size
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
				&& optimiezeCommand.getIoBuffer().getByteBuffer().remaining() < sendBufferSize) {
			optimiezeCommand = mergeBuffer(optimiezeCommand, writeQueue,
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
			// 优化get操作
			if (this.optimiezeGet) {
				optimiezeCommand = mergeGetCommands(optimiezeCommand,
						writeQueue, executingCmds, optimiezeCommand
								.getCommandType());
			}
		}
		return optimiezeCommand;
	}

	@SuppressWarnings("unchecked")
	private final Command mergeBuffer(final Command firstCommand,
			final Queue writeQueue, final Queue<Command> executingCmds,
			final int sendBufferSize) {
		Command lastCommand = firstCommand; // 合并的最后一个command
		Command nextCmd = (Command) writeQueue.peek();
		if (nextCmd == null) {
			return lastCommand;
		}

		final List<Command> commands = getLocalList();
		final ByteBuffer firstBuffer = firstCommand.getIoBuffer()
				.getByteBuffer();
		int totalBytes = firstBuffer.remaining();
		commands.add(firstCommand);
		boolean wasFirst = true;
		while (totalBytes + nextCmd.getIoBuffer().getByteBuffer().remaining() <= sendBufferSize) {
			if (nextCmd.getStatus() == OperationStatus.WRITING) {
				break;
			}
			if (nextCmd.isCancel()) {
				writeQueue.remove();
				continue;
			}
			nextCmd.setStatus(OperationStatus.WRITING);

			if (!nextCmd.getIoBuffer().getByteBuffer().hasRemaining()) {
				writeQueue.remove();
				continue;
			}

			writeQueue.remove();
			if (wasFirst) {
				wasFirst = false;
			}
			// if it is get_one command,try to merge get commands
			if ((nextCmd.getCommandType() == CommandType.GET_ONE || nextCmd
					.getCommandType() == CommandType.GETS_ONE)
					&& this.optimiezeGet) {
				nextCmd = mergeGetCommands(nextCmd, writeQueue, executingCmds,
						nextCmd.getCommandType());
			}

			commands.add(nextCmd);
			lastCommand = nextCmd;
			totalBytes += nextCmd.getIoBuffer().getByteBuffer().remaining();

			if (totalBytes > sendBufferSize) {
				break;
			}

			nextCmd = (Command) writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
		}
		if (commands.size() > 1) {
			// ArrayIoBuffer arrayBuffer = new ArrayIoBuffer(buffers);
			IoBuffer gatherBuffer = this.bufferAllocator.allocate(totalBytes);
			for (Command command : commands) {
				gatherBuffer.put(command.getIoBuffer().getByteBuffer());
				if (command != lastCommand && !command.isNoreply()) {
					executingCmds.add(command);
				}
			}
			// arrayBuffer.gathering(gatherBuffer);
			gatherBuffer.flip();
			lastCommand.setIoBuffer(gatherBuffer);

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
	}

	static class KeyStringCollector implements CommandCollector {
		final StringBuilder key = new StringBuilder();
		boolean wasFirst = true;

		@Override
		public Object getResult() {
			return this.key.toString();
		}

		@Override
		public void visit(Command command) {
			if (this.wasFirst) {
				this.key.append(command.getKey());
				this.wasFirst = false;
			} else {
				this.key.append(" ").append(command.getKey());
			}
		}

		@Override
		public void finish() {
			// do nothing

		}

	}

	class BinaryGetQCollector implements CommandCollector {
		LinkedList<IoBuffer> bufferList = new LinkedList<IoBuffer>();
		int totalLength;
		Command prevCommand;

		@Override
		public Object getResult() {
			IoBuffer mergedBuffer = Optimizer.this.bufferAllocator
					.allocate(this.totalLength);
			for (IoBuffer buffer : this.bufferList) {
				mergedBuffer.put(buffer.getByteBuffer());
			}
			mergedBuffer.flip();
			BinaryGetMultiCommand resultCommand = new BinaryGetMultiCommand(
					null, CommandType.GET_MANY, new CountDownLatch(1));
			resultCommand.setIoBuffer(mergedBuffer);
			return resultCommand;
		}

		@Override
		public void visit(Command command) {
			// Encode prev command
			if (this.prevCommand != null) {
				// first n-1 send getkq command
				Command getqCommand = new BinaryGetCommand(this.prevCommand
						.getKey(), this.prevCommand.getKeyBytes(), null, null,
						OpCode.GET_KEY_QUIETLY, true);
				getqCommand.encode(Optimizer.this.bufferAllocator);
				this.totalLength += getqCommand.getIoBuffer().remaining();
				this.bufferList.add(getqCommand.getIoBuffer());
			}
			this.prevCommand = command;
		}

		@Override
		public void finish() {
			// prev command is the last command,last command must be getk,ensure
			// getq commands send response back
			Command lastGetKCommand = new BinaryGetCommand(this.prevCommand
					.getKey(), this.prevCommand.getKeyBytes(),
					CommandType.GET_ONE, new CountDownLatch(1), OpCode.GET_KEY,
					false);
			lastGetKCommand.encode(Optimizer.this.bufferAllocator);
			this.bufferList.add(lastGetKCommand.getIoBuffer());
			this.totalLength += lastGetKCommand.getIoBuffer().remaining();
		}

	}

	@SuppressWarnings("unchecked")
	private final Command mergeGetCommands(final Command currentCmd,
			final Queue writeQueue, final Queue<Command> executingCmds,
			CommandType expectedCommandType) {
		Map<Object, Command> mergeCommands = null;
		int mergeCount = 1;
		final CommandCollector commandCollector = creatCommandCollector();
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
		commandCollector.finish();
		if (mergeCount == 1) {
			return currentCmd;
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Merge optimieze:merge " + mergeCount
						+ " get commands");
			}
			return newMergedCommand(mergeCommands, mergeCount,
					commandCollector, expectedCommandType);
		}
	}

	private CommandCollector creatCommandCollector() {
		CommandCollector commandCollector = null;
		if (this.protocol == Protocol.Text) {
			commandCollector = new KeyStringCollector();
		} else {
			commandCollector = new BinaryGetQCollector();
		}
		return commandCollector;
	}

	private Command newMergedCommand(final Map<Object, Command> mergeCommands,
			int mergeCount, final CommandCollector commandCollector,
			final CommandType commandType) {
		if (this.protocol == Protocol.Text) {
			String resultKey = (String) commandCollector.getResult();

			byte[] keyBytes = ByteUtils.getBytes(resultKey);
			byte[] cmdBytes = commandType == CommandType.GET_ONE ? Constants.GET
					: Constants.GETS;
			final IoBuffer buffer = this.bufferAllocator
					.allocate(cmdBytes.length + Constants.CRLF.length + 1
							+ keyBytes.length);
			ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
			buffer.flip();
			TextGetOneCommand cmd = new TextGetOneCommand(resultKey.toString(),
					keyBytes, commandType, null);
			cmd.setMergeCommands(mergeCommands);
			cmd.setWriteFuture(new FutureImpl<Boolean>());
			cmd.setMergeCount(mergeCount);
			cmd.setIoBuffer(buffer);
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
