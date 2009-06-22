package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedDecoder;
import net.rubyeye.xmemcached.command.VerbosityCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextVerbosityCommand extends VerbosityCommand {

	public static final String VERBOSITY = "verbosity";

	public TextVerbosityCommand(CountDownLatch latch,
			int level, boolean noreply) {
		super(latch, level, noreply);

	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("OK")) {
				setResult(Boolean.TRUE);
				countDownLatch();
				return true;
			} else
				return decodeError(line);
		}
		return false;
	}

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		final byte[] levelBytes = ByteUtils
				.getBytes(String.valueOf(this.level));
		if (isNoreply()) {
			this.ioBuffer = bufferAllocator.allocate(2 + 1 + VERBOSITY.length()
					+ levelBytes.length + 1 + Constants.NO_REPLY.length());
			ByteUtils.setArguments(this.ioBuffer, VERBOSITY, levelBytes,
					Constants.NO_REPLY);
		} else {
			this.ioBuffer = bufferAllocator.allocate(2 + 1 + VERBOSITY.length()
					+ levelBytes.length);
			ByteUtils.setArguments(this.ioBuffer, VERBOSITY, levelBytes);

		}
		this.ioBuffer.flip();
	}
}
