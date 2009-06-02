package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public interface CommandFactory {

	/**
	 * 创建delete命令
	 * 
	 * @param key
	 * @param time
	 * @return
	 */
	public abstract Command createDeleteCommand(final String key,
			final byte[] keyBytes, final int time);

	/**
	 * 创建version command
	 * 
	 * @return
	 */
	public abstract Command createVersionCommand();

	/**
	 * create flush_all command
	 * 
	 * @return
	 */
	public abstract Command createFlushAllCommand(CountDownLatch latch);

	/**
	 * create flush_all command
	 * 
	 * @return
	 */
	public abstract Command createStatsCommand(InetSocketAddress server,
			CountDownLatch latch);

	/**
	 * 创建存储命令，如set,add,replace,append,prepend,cas
	 * 
	 * @param key
	 * @param exp
	 * @param value
	 * @param cmdType
	 * @param cmd
	 * @param cas
	 *            cas值，仅对cas协议有效，其他都默认为-1
	 * @param transcoder
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public abstract Command createStoreCommand(final String key,
			final byte[] keyBytes, final int exp, final Object value,
			CommandType cmdType, final String cmd, long cas,
			Transcoder transcoder);

	/**
	 *创建get,gets命令
	 * 
	 * @param key
	 * @param keyBytes
	 * @param cmdBytes
	 *            命令的字节数组，如"get".getBytes()
	 * @param cmdType
	 *            命令类型
	 * @return
	 */
	public abstract Command createGetCommand(final String key,
			final byte[] keyBytes, final CommandType cmdType);

	/**
	 * 创建批量获取 command
	 * 
	 * @param <T>
	 * @param keys
	 * @param latch
	 * @param result
	 * @param cmdBytes
	 * @param cmdType
	 * @param transcoder
	 * @return
	 */
	public abstract <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder);

	public abstract Command createIncrDecrCommand(final String key,
			final byte[] keyBytes, final int num, CommandType cmdType);

}