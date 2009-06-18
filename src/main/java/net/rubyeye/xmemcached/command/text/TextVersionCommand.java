package net.rubyeye.xmemcached.command.text;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedDecoder;
import net.rubyeye.xmemcached.command.VersionCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class TextVersionCommand extends VersionCommand {

	public TextVersionCommand(InetSocketAddress server,CountDownLatch latch) {
		super(server,latch);

	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.startsWith("VERSION")) {
				String[] items = line.split(" ");
				setResult(items.length > 1 ? items[1] : "unknown version");
				countDownLatch();
				return true;
			} else
				decodeError(line);
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		this.ioBuffer = bufferAllocator.wrap(VERSION.slice());
	}

}
