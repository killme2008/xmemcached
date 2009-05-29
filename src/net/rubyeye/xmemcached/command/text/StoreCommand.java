package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.command.AbstractCommand;
import net.rubyeye.xmemcached.command.BooleanResultCommand;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * store command(cas,add,replace,set,append,prepend)
 *
 * @author dennis
 *
 */
public class StoreCommand extends BooleanResultCommand {
	private int exp;
	private long cas;
	private Object value;
	private String cmd;

	@SuppressWarnings("unchecked")
	public StoreCommand(final byte[] key, final int exp, final Object value,
			CommandType cmdType, final String cmd, long cas,
			Transcoder transcoder) {
		super();
		this.key = key;
		this.exp = exp;
		this.value = value;
		this.commandType = cmdType;
		this.cmd = cmd;
		this.cas = cas;
		this.transcoder = transcoder;
	}

	@Override
	public boolean decode(ByteBuffer buffer, MemcachedTCPSession session) {
		String line = session.getCurrentLine();
		if (line.equals("STORED")) {
			return notifyBoolean(session, Boolean.TRUE);
		} else if (line.equals("NOT_STORED")
				&& this.commandType != CommandType.CAS)
			return notifyBoolean(session, Boolean.FALSE);
		else if ((line.equals("EXISTS") || line.equals("NOT_FOUND"))
				&& this.commandType == CommandType.CAS) {
			return notifyBoolean(session, Boolean.FALSE);
		} else {
			session.close();
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void encode(BufferAllocator allocator) {
		final CachedData data = transcoder.encode(value);
		byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlags()));
		byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
		byte[] dataLenBytes = ByteUtils.getBytes(String
				.valueOf(data.getData().length));
		byte[] casBytes = ByteUtils.getBytes(String.valueOf(cas));
		int size = cmd.length() + 1 + key.length + 1 + flagBytes.length + 1
				+ expBytes.length + 1 + data.getData().length + 2
				* CommandFactory.CRLF.length + dataLenBytes.length;
		if (this.commandType == CommandType.CAS) {
			size += 1 + casBytes.length;
		}
		this.ioBuffer = allocator.allocate(size);
		if (this.commandType == CommandType.CAS) {
			ByteUtils.setArguments(this.ioBuffer, cmd, key, flagBytes,
					expBytes, dataLenBytes, casBytes);
		} else {
			ByteUtils.setArguments(this.ioBuffer, cmd, key, flagBytes,
					expBytes, dataLenBytes);
		}
		ByteUtils.setArguments(this.ioBuffer, data.getData());
		this.ioBuffer.flip();
	}

}
