package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.TextCommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.text.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.DeleteCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextDeleteCommand extends DeleteCommand {

	public TextDeleteCommand(String key, byte[] keyBytes, int time,
			final CountDownLatch latch) {
		super(key, keyBytes, time, latch);
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("DELETED")) {
				setResult(Boolean.TRUE);
				session.resetStatus();
				countDownLatch();
				return true;
			} else if (line.equals("NOT_FOUND")) {
				setResult(Boolean.FALSE);
				session.resetStatus();
				countDownLatch();
				return true;
			} else
				decodeError();
		}
		return false;
	}

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
		this.ioBuffer = bufferAllocator
				.allocate(TextCommandFactory.DELETE.length + 2
						+ keyBytes.length + timeBytes.length
						+ TextCommandFactory.CRLF.length);
		ByteUtils.setArguments(this.ioBuffer, TextCommandFactory.DELETE,
				keyBytes, timeBytes);
		this.ioBuffer.flip();
	}

}
