package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.command.binary.BaseBinaryCommand;
import net.rubyeye.xmemcached.command.binary.BinaryAppendPrependCommand;
import net.rubyeye.xmemcached.command.binary.BinaryDeleteCommand;
import net.rubyeye.xmemcached.command.binary.BinaryVersionCommand;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Binary protocol command factory
 * 
 * @author dennis
 * 
 */
public class BinaryCommandFactory implements CommandFactory {

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Command createDeleteCommand(String key, byte[] keyBytes, int time,
			boolean noreply) {
		return new BinaryDeleteCommand(key, keyBytes, CommandType.DELETE,
				new CountDownLatch(1), false);
	}

	@Override
	public Command createFlushAllCommand(CountDownLatch latch, int delay,
			boolean noreply) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Command createGetCommand(String key, byte[] keyBytes,
			CommandType cmdType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Command createIncrDecrCommand(String key, byte[] keyBytes, int num,
			CommandType cmdType, boolean noreply) {
		// TODO Auto-generated method stub
		return null;
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

	@SuppressWarnings("unchecked")
	final Command createStoreCommand(String key, byte[] keyBytes, int exp,
			Object value, CommandType cmdType, boolean noreply,
			Transcoder transcoder) {
		return new BaseBinaryCommand(key, keyBytes, cmdType,
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Command createVerbosityCommand(CountDownLatch latch, int level,
			boolean noreply) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Command createVersionCommand(CountDownLatch latch,
			InetSocketAddress server) {
		return new BinaryVersionCommand(latch,server);
	}

}
