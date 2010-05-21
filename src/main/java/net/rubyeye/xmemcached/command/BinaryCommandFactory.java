package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.binary.BinaryAppendPrependCommand;
import net.rubyeye.xmemcached.command.binary.BinaryAuthListMechanismsCommand;
import net.rubyeye.xmemcached.command.binary.BinaryAuthStartCommand;
import net.rubyeye.xmemcached.command.binary.BinaryAuthStepCommand;
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

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Binary protocol command factory
 * 
 * @author dennis
 * @since 1.2.0
 */
@SuppressWarnings("unchecked")
public class BinaryCommandFactory implements CommandFactory {

	private BufferAllocator bufferAllocator = new SimpleBufferAllocator();

	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	public Command createAddCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		return createStoreCommand(key, keyBytes, exp, value, CommandType.ADD,
				noreply, transcoder);
	}

	public Command createAppendCommand(String key, byte[] keyBytes,
			Object value, boolean noreply, Transcoder transcoder) {
		return new BinaryAppendPrependCommand(key, keyBytes,
				CommandType.APPEND, new CountDownLatch(1), 0, 0, value,
				noreply, transcoder);
	}

	public Command createCASCommand(String key, byte[] keyBytes, int exp,
			Object value, long cas, boolean noreply, Transcoder transcoder) {
		return new BinaryCASCommand(key, keyBytes, CommandType.CAS,
				new CountDownLatch(1), exp, cas, value, noreply, transcoder);
	}

	public Command createDeleteCommand(String key, byte[] keyBytes, int time,
			boolean noreply) {
		return new BinaryDeleteCommand(key, keyBytes, CommandType.DELETE,
				new CountDownLatch(1), noreply);
	}

	public Command createFlushAllCommand(CountDownLatch latch, int delay,
			boolean noreply) {
		return new BinaryFlushAllCommand(latch, delay, noreply);
	}

	public Command createGetCommand(String key, byte[] keyBytes,
			CommandType cmdType, Transcoder transcoder) {
		return new BinaryGetCommand(key, keyBytes, cmdType, new CountDownLatch(
				1), OpCode.GET, false);
	}

	public <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder) {
		Iterator<String> it = keys.iterator();
		String key = null;
		List<com.google.code.yanf4j.buffer.IoBuffer> bufferList = new ArrayList<com.google.code.yanf4j.buffer.IoBuffer>();
		int totalLength = 0;
		while (it.hasNext()) {
			key = it.next();
			if (it.hasNext()) {
				// first n-1 send getq command
				Command command = new BinaryGetCommand(key, ByteUtils
						.getBytes(key), cmdType, null, OpCode.GET_KEY_QUIETLY,
						true);
				command.encode();
				totalLength += command.getIoBuffer().remaining();
				bufferList.add(command.getIoBuffer());
			}
		}
		// last key,create a get command
		Command lastCommand = new BinaryGetCommand(key,
				ByteUtils.getBytes(key), cmdType, new CountDownLatch(1),
				OpCode.GET_KEY, false);
		lastCommand.encode();
		bufferList.add(lastCommand.getIoBuffer());
		totalLength += lastCommand.getIoBuffer().remaining();

		IoBuffer mergedBuffer = IoBuffer.allocate(totalLength);
		for (IoBuffer buffer : bufferList) {
			mergedBuffer.put(buffer.buf());
		}
		mergedBuffer.flip();
		Command resultCommand = new BinaryGetMultiCommand(key, cmdType, latch);
		resultCommand.setIoBuffer(mergedBuffer);
		return resultCommand;
	}

	public Command createIncrDecrCommand(String key, byte[] keyBytes,
			long amount, long initial, int expTime, CommandType cmdType,
			boolean noreply) {
		return new BinaryIncrDecrCommand(key, keyBytes, amount, initial,
				expTime, cmdType, noreply);
	}

	public Command createPrependCommand(String key, byte[] keyBytes,
			Object value, boolean noreply, Transcoder transcoder) {
		return new BinaryAppendPrependCommand(key, keyBytes,
				CommandType.PREPEND, new CountDownLatch(1), 0, 0, value,
				noreply, transcoder);
	}

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

	public Command createSetCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		return createStoreCommand(key, keyBytes, exp, value, CommandType.SET,
				noreply, transcoder);
	}

	public Command createStatsCommand(InetSocketAddress server,
			CountDownLatch latch, String itemName) {
		return new BinaryStatsCommand(server, latch, itemName);
	}

	public Command createVerbosityCommand(CountDownLatch latch, int level,
			boolean noreply) {
		throw new UnsupportedOperationException(
				"Binary protocol doesn't support verbosity");
	}

	public Command createVersionCommand(CountDownLatch latch,
			InetSocketAddress server) {
		return new BinaryVersionCommand(latch, server);
	}

	public Command createAuthListMechanismsCommand(CountDownLatch latch) {
		return new BinaryAuthListMechanismsCommand(latch);
	}

	public Command createAuthStartCommand(String mechanism,
			CountDownLatch latch, byte[] authData) {
		return new BinaryAuthStartCommand(mechanism, ByteUtils
				.getBytes(mechanism), latch, authData);
	}

	public Command createAuthStepCommand(String mechanism,
			CountDownLatch latch, byte[] authData) {
		return new BinaryAuthStepCommand(mechanism, ByteUtils
				.getBytes(mechanism), latch, authData);
	}

	public Protocol getProtocol() {
		return Protocol.Binary;
	}

}
