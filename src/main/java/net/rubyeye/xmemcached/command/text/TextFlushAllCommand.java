package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedDecoder;
import net.rubyeye.xmemcached.command.FlushAllCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextFlushAllCommand extends FlushAllCommand {

	public TextFlushAllCommand(CountDownLatch latch, int delay, boolean noreply) {
		super(latch, delay, noreply);

	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("OK")) {
				setResult(Boolean.TRUE);
				countDownLatch();
				return true;
			} else
				decodeError(line);
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		if (isNoreply()) {
			this.ioBuffer = bufferAllocator.allocate("flush_all".length() + 1
					+ Constants.NO_REPLY.length() + 2);
			ByteUtils.setArguments(this.ioBuffer, "flush_all",
					Constants.NO_REPLY);

			this.ioBuffer.flip();
		} else
			this.ioBuffer = bufferAllocator.wrap(FLUSH_ALL.slice());
	}

}
