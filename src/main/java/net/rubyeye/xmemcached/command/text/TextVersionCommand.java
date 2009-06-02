package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.VersionCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class TextVersionCommand extends VersionCommand {

	public TextVersionCommand(CountDownLatch latch) {
		super(latch);

	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.startsWith("VERSION")) {
				String[] items = line.split(" ");
				setResult(items.length > 1 ? items[1] : "unknown version");
				countDownLatch();
				return true;
			} else
				decodeError();
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		this.ioBuffer = bufferAllocator.wrap(VERSION.slice());
	}

}
