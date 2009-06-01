package net.rubyeye.xmemcached;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleIoBuffer;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * Command Factory for creating text protocol commands.
 *
 * @author dennis
 *
 */
public final class TextCommandFactory {

	private static BufferAllocator bufferAllocator = new SimpleBufferAllocator();

	public static final void setBufferAllocator(BufferAllocator allocator) {
		BufferAllocator oldAllocator = bufferAllocator;
		bufferAllocator = allocator;
		if (oldAllocator != null) {
			oldAllocator.dispose();
		}
	}

	/**
	 * 创建delete命令
	 *
	 * @param key
	 * @param time
	 * @return
	 */
	public static final Command createDeleteCommand(final String key,
			final byte[] keyBytes, final int time) {
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
		final IoBuffer buffer = bufferAllocator
				.allocate(TextCommandFactory.DELETE.length + 2 + keyBytes.length
						+ timeBytes.length + TextCommandFactory.CRLF.length);
		ByteUtils.setArguments(buffer, TextCommandFactory.DELETE, keyBytes,
				timeBytes);
		buffer.flip();
		Command command = new Command(key, CommandType.DELETE, latch);
		command.setIoBuffer(buffer);
		return command;
	}

	/**
	 * 创建version command
	 *
	 * @return
	 */
	public static final Command createVersionCommand() {
		final CountDownLatch latch = new CountDownLatch(1);
		final IoBuffer buffer = new SimpleIoBuffer(VERSION.slice());
		Command command = new Command("version", CommandType.VERSION,
				latch);
		command.setIoBuffer(buffer);
		return command;
	}

	/**
	 * create flush_all command
	 *
	 * @return
	 */
	public static final Command createFlushAllCommand() {
		final IoBuffer buffer = new SimpleIoBuffer(FLUSH_ALL.slice());
		Command command = new Command("flush_all",
				CommandType.FLUSH_ALL, null);
		command.setIoBuffer(buffer);
		return command;
	}

	/**
	 * create flush_all command
	 *
	 * @return
	 */
	public static final Command createStatsCommand() {
		final IoBuffer buffer = new SimpleIoBuffer(STATS.slice());
		Command command = new Command("stats", CommandType.STATS, null);
		command.setIoBuffer(buffer);
		return command;
	}

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
	public static final Command createStoreCommand(final String key,
			final byte[] keyBytes, final int exp, final Object value,
			CommandType cmdType, final String cmd, long cas,
			Transcoder transcoder) {
		final CountDownLatch latch = new CountDownLatch(1);
		final CachedData data = transcoder.encode(value);
		byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlags()));
		byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
		byte[] dataLenBytes = ByteUtils.getBytes(String
				.valueOf(data.getData().length));
		byte[] casBytes = ByteUtils.getBytes(String.valueOf(cas));
		int size = cmd.length() + 1 + keyBytes.length + 1 + flagBytes.length
				+ 1 + expBytes.length + 1 + data.getData().length + 2
				* TextCommandFactory.CRLF.length + dataLenBytes.length;
		if (cmdType == CommandType.CAS) {
			size += 1 + casBytes.length;
		}
		final IoBuffer buffer = bufferAllocator.allocate(size);
		if (cmdType == CommandType.CAS) {
			ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
					dataLenBytes, casBytes);
		} else {
			ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
					dataLenBytes);
		}
		ByteUtils.setArguments(buffer, data.getData());
		buffer.flip();
		Command command = new Command(key, cmdType, latch);
		command.setIoBuffer(buffer);
		return command;
	}

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
	public static final Command createGetCommand(final String key,
			final byte[] keyBytes, final byte[] cmdBytes,
			final CommandType cmdType) {
		final CountDownLatch latch = new CountDownLatch(1);
		final IoBuffer buffer = bufferAllocator.allocate(cmdBytes.length
				+ TextCommandFactory.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
		buffer.flip();
		Command command = new Command(key, cmdType, latch);
		command.setIoBuffer(buffer);
		return command;
	}

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
	public static final <T> Command createGetMultiCommand(
			Collection<String> keys, CountDownLatch latch,
			Collection<Map<String, CachedData>> result, byte[] cmdBytes,
			CommandType cmdType, Transcoder<T> transcoder) {
		final Command command = new Command(keys.iterator().next(), cmdType,
				latch);
		command.setResult(result); // 共用一个result map
		command.setTranscoder(transcoder);
		StringBuilder sb = new StringBuilder(keys.size() * 5);
		for (String tmpKey : keys) {
			ByteUtils.checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		String gatherKey = sb.toString();
		byte[] keyBytes = ByteUtils.getBytes(gatherKey.substring(0, gatherKey
				.length() - 1));
		final IoBuffer buffer = bufferAllocator.allocate(cmdBytes.length
				+ TextCommandFactory.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
		buffer.flip();
		command.setIoBuffer(buffer);
		return command;
	}

	public static final Command createIncrDecrCommand(final String key,
			final byte[] keyBytes, final int num, CommandType cmdType,
			final String cmd) {
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] numBytes = ByteUtils.getBytes(String.valueOf(num));
		byte[] cmdBytes = ByteUtils.getBytes(cmd);
		final IoBuffer buffer = bufferAllocator.allocate(cmd.length() + 2
				+ key.length() + numBytes.length + TextCommandFactory.CRLF.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes, numBytes);
		buffer.flip();
		Command command = new Command(key, cmdType, latch);
		command.setIoBuffer(buffer);
		return command;
	}

	public static final byte[] CRLF = { '\r', '\n' };
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't', 's' };
	public static final byte[] DELETE = { 'd', 'e', 'l', 'e', 't', 'e' };
	public static final byte SPACE = ' ';
	public static final ByteBuffer STATS = ByteBuffer.wrap("stats\r\n"
			.getBytes());
	public static final ByteBuffer FLUSH_ALL = ByteBuffer.wrap("flush_all\r\n"
			.getBytes());
	public static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n"
			.getBytes());
	public static final String SPLIT = "\r\n";
}
