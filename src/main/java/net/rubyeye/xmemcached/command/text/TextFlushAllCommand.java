package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextFlushAllCommand extends Command {

	public static final ByteBuffer FLUSH_ALL = ByteBuffer.wrap("flush_all\r\n"
			.getBytes());

	protected int exptime;

	public final int getExptime() {
		return this.exptime;
	}

	public TextFlushAllCommand(final CountDownLatch latch, int delay,
			boolean noreply) {
		super("flush_all", (byte[]) null, latch);
		this.commandType = CommandType.FLUSH_ALL;
		this.exptime = delay;
		this.noreply = noreply;
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining()) {
			return false;
		}
		if (this.result == null) {
			byte first = buffer.get(buffer.position());
			if (first == 'O') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// OK\r\n
				return ByteUtils.stepBuffer(buffer, 4);
			} else {
				return decodeError(session, buffer);
			}
		} else {
			return ByteUtils.stepBuffer(buffer, 4);
		}
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		if (isNoreply()) {
			if (this.exptime <= 0) {
				this.ioBuffer = bufferAllocator.allocate("flush_all".length()
						+ 1 + Constants.NO_REPLY.length() + 2);
				ByteUtils.setArguments(this.ioBuffer, "flush_all",
						Constants.NO_REPLY);
			} else {
				byte[] delayBytes = ByteUtils.getBytes(String
						.valueOf(this.exptime));
				this.ioBuffer = bufferAllocator.allocate("flush_all".length()
						+ 2 + delayBytes.length + Constants.NO_REPLY.length()
						+ 2);
				ByteUtils.setArguments(this.ioBuffer, "flush_all", delayBytes,
						Constants.NO_REPLY);
			}
			this.ioBuffer.flip();
		} else {
			if (this.exptime <= 0) {
				this.ioBuffer = bufferAllocator.wrap(FLUSH_ALL.slice());
			} else {

				byte[] delayBytes = ByteUtils.getBytes(String
						.valueOf(this.exptime));
				this.ioBuffer = bufferAllocator.allocate("flush_all".length()
						+ 1 + delayBytes.length + 2);
				ByteUtils.setArguments(this.ioBuffer, "flush_all", delayBytes);
			}
		}
	}

}
