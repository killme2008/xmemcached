package net.rubyeye.xmemcached.command.kestrel;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.text.TextFlushAllCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * Kestrel flush command
 * 
 * @author dennis
 * 
 */
public class KestrelFlushAllCommand extends TextFlushAllCommand {

	public KestrelFlushAllCommand(CountDownLatch latch, int delay,
			boolean noreply) {
		super(latch, delay, noreply);
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining()) {
			return false;
		}
		String line = ByteUtils.nextLine(buffer);
		if (line == null) {
			return false;
		} else {
			if (line.startsWith("Flushed")) {
				setResult(Boolean.TRUE);
				countDownLatch();
				return true;
			} else {
				return decodeError(session, buffer);
			}
		}
	}

}
