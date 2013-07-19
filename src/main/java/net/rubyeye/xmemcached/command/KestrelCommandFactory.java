package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.kestrel.KestrelDeleteCommand;
import net.rubyeye.xmemcached.command.kestrel.KestrelFlushAllCommand;
import net.rubyeye.xmemcached.command.kestrel.KestrelGetCommand;
import net.rubyeye.xmemcached.command.kestrel.KestrelSetCommand;
import net.rubyeye.xmemcached.command.text.TextQuitCommand;
import net.rubyeye.xmemcached.command.text.TextStatsCommand;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.Protocol;

/**
 * Kestrel is a message queue written in scala by
 * robey(http://github.com/robey/kestrel).It's protocol use memcached text
 * protocol,so you can use any memcached clients to talk with it.But it's
 * protocol implementation is not all compatible with memcached standard
 * protocol,So xmemcached supply this command factory for it.
 * 
 * @author dennis
 * 
 */
@SuppressWarnings("unchecked")
public class KestrelCommandFactory implements CommandFactory {

	public Command createAddCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createAppendCommand(String key, byte[] keyBytes,
			Object value, boolean noreply, Transcoder transcoder) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createCASCommand(String key, byte[] keyBytes, int exp,
			Object value, long cas, boolean noreply, Transcoder transcoder) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createDeleteCommand(String key, byte[] keyBytes, int time,
			long cas,
			boolean noreply) {
		return new KestrelDeleteCommand(key, keyBytes, -1,
				new CountDownLatch(1), noreply);
	}

	public Command createFlushAllCommand(CountDownLatch latch, int delay,
			boolean noreply) {
		return new KestrelFlushAllCommand(latch, delay, noreply);
	}

	public Command createGetCommand(String key, byte[] keyBytes,
			CommandType cmdType, Transcoder transcoder) {
		return new KestrelGetCommand(key, keyBytes, cmdType,
				new CountDownLatch(1), transcoder);
	}

	public <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createIncrDecrCommand(String key, byte[] keyBytes,
			long amount, long initial, int expTime, CommandType cmdType,
			boolean noreply) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createPrependCommand(String key, byte[] keyBytes,
			Object value, boolean noreply, Transcoder transcoder) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createReplaceCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createSetCommand(String key, byte[] keyBytes, int exp,
			Object value, boolean noreply, Transcoder transcoder) {
		return new KestrelSetCommand(key, keyBytes, CommandType.SET,
				new CountDownLatch(1), exp, -1, value, noreply, transcoder);
	}

	public Command createStatsCommand(InetSocketAddress server,
			CountDownLatch latch, String itemName) {
		if (itemName != null) {
			throw new UnsupportedOperationException(
					"Kestrel doesn't support 'stats itemName'");
		}
		return new TextStatsCommand(server, latch, null);
	}

	public Command createVerbosityCommand(CountDownLatch latch, int level,
			boolean noreply) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createVersionCommand(CountDownLatch latch,
			InetSocketAddress server) {
		throw new UnsupportedOperationException(
				"Kestrel doesn't support this operation");
	}

	public Command createQuitCommand() {
		return new TextQuitCommand();
	}

	public Protocol getProtocol() {
		return Protocol.Kestrel;
	}

	public Command createAuthListMechanismsCommand(CountDownLatch latch) {
		throw new UnsupportedOperationException("Kestrel doesn't support SASL");
	}

	public Command createAuthStartCommand(String mechanism,
			CountDownLatch latch, byte[] authData) {
		throw new UnsupportedOperationException("Kestrel doesn't support SASL");
	}

	public Command createAuthStepCommand(String mechanism,
			CountDownLatch latch, byte[] authData) {
		throw new UnsupportedOperationException("Kestrel doesn't support SASL");
	}

	public Command createGetAndTouchCommand(String key, byte[] keyBytes,
			CountDownLatch latch, int exp, boolean noreply) {
		throw new UnsupportedOperationException(
				"GAT is only supported by binary protocol");
	}

	public Command createTouchCommand(String key, byte[] keyBytes, CountDownLatch latch,
			int exp, boolean noreply) {
		throw new UnsupportedOperationException(
				"Touch is only supported by binary protocol");
	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {

	}

}
