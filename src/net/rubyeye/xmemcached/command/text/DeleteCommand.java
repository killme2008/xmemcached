package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.BooleanResultCommand;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * delete command
 *
 * @author dennis
 *
 */
public class DeleteCommand extends BooleanResultCommand {
	private int time;

	public DeleteCommand(byte[] key, int time) {
		super();
		this.key = key;
		this.time = time;
		this.commandType=CommandType.DELETE;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	@Override
	public boolean decode(ByteBuffer buffer, MemcachedTCPSession session) {
		String line = session.getCurrentLine();
		if (line.equals("DELETED")) {
			return notifyBoolean(session, Boolean.TRUE);
		} else if (line.equals("NOT_FOUND")) {
			return notifyBoolean(session, Boolean.FALSE);
		} else {
			session.close();
			return false;
		}
	}

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
		this.ioBuffer = bufferAllocator.allocate(CommandFactory.DELETE.length
				+ 2 + key.length + timeBytes.length
				+ CommandFactory.CRLF.length);
		ByteUtils.setArguments(this.ioBuffer, CommandFactory.DELETE, key,
				timeBytes);
		this.ioBuffer.flip();
	}

}
