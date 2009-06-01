package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.text.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.FlushAllCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class TextFlushAllCommand extends FlushAllCommand {

	public TextFlushAllCommand(CountDownLatch latch) {
		super(latch);

	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("OK")) {
				setResult(Boolean.TRUE);
				countDownLatch();
				session.resetStatus();
				return true;
			} else
				decodeError();
		}
		return false;
	}

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		this.ioBuffer = bufferAllocator.wrap(FLUSH_ALL.slice());
	}

}
