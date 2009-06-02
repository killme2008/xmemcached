package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextCASCommand;
import net.rubyeye.xmemcached.command.text.TextDeleteCommand;
import net.rubyeye.xmemcached.command.text.TextFlushAllCommand;
import net.rubyeye.xmemcached.command.text.TextGetManyCommand;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;
import net.rubyeye.xmemcached.command.text.TextIncrDecrCommand;
import net.rubyeye.xmemcached.command.text.TextStatsCommand;
import net.rubyeye.xmemcached.command.text.TextStoreCommand;
import net.rubyeye.xmemcached.command.text.TextVersionCommand;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * Command Factory for creating text protocol commands.
 * 
 * @author dennis
 * 
 */
public final class TextCommandFactory implements CommandFactory {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.CommandFactory#createDeleteCommand(java.lang.String
	 * , byte[], int)
	 */
	public final Command createDeleteCommand(final String key,
			final byte[] keyBytes, final int time) {
		return new TextDeleteCommand(key, keyBytes, time, new CountDownLatch(1));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.CommandFactory#createVersionCommand()
	 */
	public final Command createVersionCommand() {
		return new TextVersionCommand(new CountDownLatch(1));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.CommandFactory#createFlushAllCommand(java.util
	 * .concurrent.CountDownLatch)
	 */
	public final Command createFlushAllCommand(CountDownLatch latch) {
		return new TextFlushAllCommand(latch);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.rubyeye.xmemcached.CommandFactory#createStatsCommand(java.net.
	 * InetSocketAddress, java.util.concurrent.CountDownLatch)
	 */
	public final Command createStatsCommand(InetSocketAddress server,
			CountDownLatch latch) {
		return new TextStatsCommand(server, latch);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.CommandFactory#createStoreCommand(java.lang.String
	 * , byte[], int, java.lang.Object,
	 * net.rubyeye.xmemcached.command.CommandType, java.lang.String, long,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	@SuppressWarnings("unchecked")
	public final Command createStoreCommand(final String key,
			final byte[] keyBytes, final int exp, final Object value,
			CommandType cmdType, final String cmd, long cas,
			Transcoder transcoder) {
		if (cmdType == CommandType.CAS)
			return new TextCASCommand(key, keyBytes, cmdType,
					new CountDownLatch(1), exp, cas, value, transcoder, cmd);
		else
			return new TextStoreCommand(key, keyBytes, cmdType,
					new CountDownLatch(1), exp, cas, value, transcoder, cmd);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.CommandFactory#createGetCommand(java.lang.String,
	 * byte[], net.rubyeye.xmemcached.command.CommandType)
	 */
	public final Command createGetCommand(final String key,
			final byte[] keyBytes, final CommandType cmdType) {
		return new TextGetOneCommand(key, keyBytes, cmdType,
				new CountDownLatch(1));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.CommandFactory#createGetMultiCommand(java.util
	 * .Collection, java.util.concurrent.CountDownLatch,
	 * net.rubyeye.xmemcached.command.CommandType,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder) {
		StringBuilder sb = new StringBuilder(keys.size() * 5);
		for (String tmpKey : keys) {
			ByteUtils.checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		String gatherKey = sb.toString();
		byte[] keyBytes = ByteUtils.getBytes(gatherKey.substring(0, gatherKey
				.length() - 1));
		return new TextGetManyCommand(keys.iterator().next(), keyBytes,
				cmdType, latch, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.CommandFactory#createIncrDecrCommand(java.lang
	 * .String, byte[], int, net.rubyeye.xmemcached.command.CommandType)
	 */
	public final Command createIncrDecrCommand(final String key,
			final byte[] keyBytes, final int num, CommandType cmdType) {
		return new TextIncrDecrCommand(key, keyBytes, cmdType,
				new CountDownLatch(1), num);
	}
}
