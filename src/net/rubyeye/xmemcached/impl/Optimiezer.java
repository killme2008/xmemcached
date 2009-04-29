/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rubyeye.xmemcached.impl;

import net.rubyeye.xmemcached.*;
import com.google.code.yanf4j.util.Queue;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.Deque;

/**
 * 
 * @author dennis
 */
public class Optimiezer implements OptimiezerMBean, MemcachedOptimiezer {

	public static final int DEFAULT_MERGE_FACTOR = 150;
	private int mergeFactor = DEFAULT_MERGE_FACTOR; // default merge factor;
	private boolean optimiezeGet = true;
	private boolean optimiezeMergeBuffer = true;
	BufferAllocator bufferAllocator;

	public Optimiezer() {
		XMemcachedMbeanServer.getInstance().registMBean(
				this,
				this.getClass().getPackage().getName() + ":type="
						+ this.getClass().getSimpleName());
	}

	public BufferAllocator getBufferAllocator() {
		return bufferAllocator;
	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	public int getMergeFactor() {
		return mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		this.mergeFactor = mergeFactor;
	}

	public boolean isOptimiezeGet() {
		return optimiezeGet;
	}

	public void setOptimiezeGet(boolean optimiezeGet) {
		this.optimiezeGet = optimiezeGet;
	}

	public boolean isOptimiezeMergeBuffer() {
		return optimiezeMergeBuffer;
	}

	public void setOptimiezeMergeBuffer(boolean optimiezeMergeBuffer) {
		this.optimiezeMergeBuffer = optimiezeMergeBuffer;
	}

	@SuppressWarnings("unchecked")
	public Command optimieze(final Command currentCommand,
			final Queue writeQueue, final BlockingQueue<Command> executingCmds,
			int sendBufferSize) {
		Command optimiezeCommand = currentCommand;
		if (optimiezeCommand.getCommandType() == Command.CommandType.GET_ONE) {
			final List<Command> mergeCommands = new ArrayList<Command>(
					mergeFactor / 2);
			mergeCommands.add(optimiezeCommand);
			// 优化get操作
			if (optimiezeGet) {
				writeQueue.remove();
				optimiezeCommand = optimizeGet(optimiezeCommand, writeQueue,
						executingCmds, mergeCommands);
				((Deque) writeQueue).addFirst(optimiezeCommand); // 加入队首
			}
		}
		if (optimiezeMergeBuffer
				&& optimiezeCommand.getIoBuffer().getByteBuffer().remaining() < sendBufferSize) {
			writeQueue.remove();
			optimiezeCommand = optimiezeBuffer(optimiezeCommand, writeQueue,
					executingCmds, sendBufferSize);
			((Deque) writeQueue).addFirst(optimiezeCommand);// 加入队首

		}
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
	private Command optimiezeBuffer(final Command currentCommand,
			final Queue writeQueue, final BlockingQueue<Command> executingCmds,
			int sendBufferSize) {
		final List<ByteBuffer> buffers = getLocalList();
		final ByteBuffer endBuffer = currentCommand.getIoBuffer()
				.getByteBuffer();
		int totalBytes = endBuffer.remaining();
		while (totalBytes < sendBufferSize && buffers.size() < mergeFactor) {
			Command nextCmd = (Command) writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
			if (nextCmd.getStatus() == OperationStatus.WRITING) {
				break;
			}
			if (nextCmd.isCancel()) {
				writeQueue.remove();
				continue;
			}
			nextCmd.setStatus(OperationStatus.WRITING);
			final ByteBuffer buff = nextCmd.getIoBuffer().getByteBuffer();
			if (!buff.hasRemaining()) {
				writeQueue.remove();
				continue;
			}
			buffers.add(buff);
			totalBytes += buff.remaining();
			writeQueue.remove();
			executingCmds.add(nextCmd);
		}
		if (buffers.size() > 0) {
			buffers.add(endBuffer);// current buffer add to end

			// ArrayIoBuffer arrayBuffer = new ArrayIoBuffer(buffers);
			IoBuffer gatherBuffer = this.bufferAllocator.allocate(totalBytes);
			for (ByteBuffer buffer : buffers) {
				gatherBuffer.put(buffer);
			}
			// arrayBuffer.gathering(gatherBuffer);
			gatherBuffer.flip();
			currentCommand.setIoBuffer(gatherBuffer);
		}
		return currentCommand;
	}

	private ThreadLocal<List<ByteBuffer>> threadLocal = new ThreadLocal<List<ByteBuffer>>() {

		@Override
		protected List<ByteBuffer> initialValue() {
			return new ArrayList<ByteBuffer>(mergeFactor);
		}
	};

	public List<ByteBuffer> getLocalList() {
		List<ByteBuffer> list = threadLocal.get();
		list.clear();
		return list;
	}

	/**
	 * 优化get操作，连续的get操作将合并成一个
	 * 
	 * @param currentCmd
	 * @param mergeCommands
	 * @return
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	private Command optimizeGet(final Command currentCmd,
			final Queue writeQueue, final BlockingQueue<Command> executingCmds,
			final List<Command> mergeCommands) {
		int mergeCount = 1;
		final StringBuilder key = new StringBuilder();
		currentCmd.setStatus(OperationStatus.WRITING);
		key.append((String) currentCmd.getKey());
		while (mergeCount <= mergeFactor) {
			Command nextCmd = (Command) writeQueue.peek();
			if (nextCmd == null) {
				break;
			}
			if (nextCmd.isCancel()) {
				writeQueue.remove();
				continue;
			}
			if (nextCmd.getCommandType() == Command.CommandType.GET_ONE) {
				nextCmd.setStatus(OperationStatus.WRITING);
				mergeCommands.add((Command) writeQueue.remove());
				key.append(" ").append((String) nextCmd.getKey());
				mergeCount++;
			} else {
				break;
			}
		}
		if (mergeCount == 1) {
			return currentCmd;
		} else {
			return newMergedCommand(mergeCommands, key);
		}
	}

	private Command newMergedCommand(final List<Command> mergeCommands,
			final StringBuilder key) {
		byte[] keyBytes = ByteUtils.getBytes(key.toString());
		final IoBuffer buffer = bufferAllocator.allocate(ByteUtils.GET.length
				+ ByteUtils.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, ByteUtils.GET, keyBytes);
		buffer.flip();
		Command cmd = new Command(key.toString(), Command.CommandType.GET_ONE,
				null) {

			public List<Command> getMergeCommands() {
				return mergeCommands;
			}
		};
		cmd.setMergeCount(mergeCommands.size());
		cmd.setIoBuffer(buffer);
		return cmd;
	}
}
