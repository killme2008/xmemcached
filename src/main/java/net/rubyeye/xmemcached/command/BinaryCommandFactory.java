package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.command.binary.BinaryAppendPrependCommand;
import net.rubyeye.xmemcached.command.binary.BinaryCASCommand;
import net.rubyeye.xmemcached.command.binary.BinaryDeleteCommand;
import net.rubyeye.xmemcached.command.binary.BinaryFlushAllCommand;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.BinaryGetMultiCommand;
import net.rubyeye.xmemcached.command.binary.BinaryIncrDecrCommand;
import net.rubyeye.xmemcached.command.binary.BinaryStatsCommand;
import net.rubyeye.xmemcached.command.binary.BinaryStoreCommand;
import net.rubyeye.xmemcached.command.binary.BinaryVersionCommand;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.Protocol;

/**
 * Binary protocol command factory
 * 
 * @author dennis
 * @since 1.2.0
 */
@SuppressWarnings("unchecked")
public class BinaryCommandFactory implements CommandFactory {

	private BufferAllocator bufferAllocator;

	@Override
	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	@Override
	public Command createAddCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		return createStoreCommand(key, keyBytes, exp, value, CommandType.ADD,
				noreply, transcoder);
	}

	@Override
	public Command createAppendCommand(String key, byte[] keyBytes,
			Object value, boolean noreply, Transcoder transcoder) {
		return new BinaryAppendPrependCommand(key, keyBytes,
				CommandType.APPEND, new CountDownLatch(1), 0, 0, value,
				noreply, transcoder);
	}

	@Override
	public Command createCASCommand(String key, byte[] keyBytes, int exp,
			Object value, long cas, boolean noreply, Transcoder transcoder) {
		return new BinaryCASCommand(key, keyBytes, CommandType.CAS,
				new CountDownLatch(1), exp, cas, value, noreply, transcoder);
	}

	@Override
	public Command createDeleteCommand(String key, byte[] keyBytes, int time,
			boolean noreply) {
		return new BinaryDeleteCommand(key, keyBytes, CommandType.DELETE,
				new CountDownLatch(1), noreply);
	}

	@Override
	public Command createFlushAllCommand(CountDownLatch latch, int delay,
			boolean noreply) {
		return new BinaryFlushAllCommand(latch, delay, noreply);
	}

	@Override
	public Command createGetCommand(String key, byte[] keyBytes,
			CommandType cmdType) {
		return new BinaryGetCommand(key, keyBytes, cmdType, new CountDownLatch(
				1), OpCode.GET, false);
	}

	@Override
	public <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder) {
		Iterator<String> it = keys.iterator();
		String key = null;
		List<IoBuffer> bufferList = new ArrayList<IoBuffer>();
		int totalLength = 0;
		while (it.hasNext()) {
			key = it.next();
			if (it.hasNext()) {
				// first n-1 send getkq command
				Command command = new BinaryGetCommand(key, ByteUtils
						.getBytes(key), cmdType, null, OpCode.GET_KEY_QUIETLY,
						true);
				command.encode(this.bufferAllocator);
				totalLength += command.getIoBuffer().remaining();
				bufferList.add(command.getIoBuffer());
			}
		}
		// last key,create a getk command
		Command lastCommand = new BinaryGetCommand(key,
				ByteUtils.getBytes(key), cmdType, new CountDownLatch(1),
				OpCode.GET_KEY, false);
		lastCommand.encode(this.bufferAllocator);
		bufferList.add(lastCommand.getIoBuffer());
		totalLength += lastCommand.getIoBuffer().remaining();

		IoBuffer mergedBuffer = this.bufferAllocator.allocate(totalLength);
		for (IoBuffer buffer : bufferList) {
			mergedBuffer.put(buffer.getByteBuffer());
		}
		mergedBuffer.flip();
		Command resultCommand = new BinaryGetMultiCommand(key, cmdType, latch);
		resultCommand.setIoBuffer(mergedBuffer);
		return resultCommand;
	}

	@Override
	public Command createIncrDecrCommand(String key, byte[] keyBytes,
			int amount, int initial, int expTime, CommandType cmdType,
			boolean noreply) {
		return new BinaryIncrDecrCommand(key, keyBytes, amount, initial,
				expTime, cmdType, noreply);
	}

	@Override
	public Command createPrependCommand(String key, byte[] keyBytes,
			Object value, boolean noreply, Transcoder transcoder) {
		return new BinaryAppendPrependCommand(key, keyBytes,
				CommandType.PREPEND, new CountDownLatch(1), 0, 0, value,
				noreply, transcoder);
	}

	@Override
	public Command createReplaceCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		return createStoreCommand(key, keyBytes, exp, value,
				CommandType.REPLACE, noreply, transcoder);
	}

	final Command createStoreCommand(String key, byte[] keyBytes, int exp,
			Object value, CommandType cmdType, boolean noreply,
			Transcoder transcoder) {
		return new BinaryStoreCommand(key, keyBytes, cmdType,
				new CountDownLatch(1), exp, -1, value, noreply, transcoder);
	}

	@Override
	public Command createSetCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		return createStoreCommand(key, keyBytes, exp, value, CommandType.SET,
				noreply, transcoder);
	}

	@Override
	public Command createStatsCommand(InetSocketAddress server,
			CountDownLatch latch, String itemName) {
		return new BinaryStatsCommand(server, latch, itemName);
	}

	@Override
	public Command createVerbosityCommand(CountDownLatch latch, int level,
			boolean noreply) {
		throw new UnsupportedOperationException(
				"Binary protocol doesn't support verbosity");
	}

	@Override
	public Command createVersionCommand(CountDownLatch latch,
			InetSocketAddress server) {
		return new BinaryVersionCommand(latch, server);
	}

	@Override
	public Protocol getProtocol() {
		return Protocol.Binary;
	}

}
