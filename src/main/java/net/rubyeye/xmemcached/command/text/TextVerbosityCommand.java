package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.VerbosityCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextVerbosityCommand extends VerbosityCommand {

	public static final String VERBOSITY = "verbosity";

	public TextVerbosityCommand(CountDownLatch latch, int level, boolean noreply) {
		super(latch, level, noreply);

	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining())
			return false;
		if (result == null) {
			byte first = buffer.get(buffer.position());
			if (first == 'O') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// OK\r\n
				return ByteUtils.stepBuffer(buffer, 4);
			} else
				return decodeError(session, buffer);
		} else {
			return ByteUtils.stepBuffer(buffer, 4);
		}
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
