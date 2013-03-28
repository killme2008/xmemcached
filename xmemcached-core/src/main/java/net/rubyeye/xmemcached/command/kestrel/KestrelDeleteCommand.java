package net.rubyeye.xmemcached.command.kestrel;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.text.TextDeleteCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class KestrelDeleteCommand extends TextDeleteCommand {

	public KestrelDeleteCommand(String key, byte[] keyBytes, int time,
			CountDownLatch latch, boolean noreply) {
		super(key, keyBytes, time, latch, noreply);
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining())
			return false;
		if (this.result == null) {
			if (buffer.remaining() < 2)
				return false;
			byte first = buffer.get(buffer.position());
			byte second = buffer.get(buffer.position() + 1);
			if (first == 'E' && second == 'N') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// END\r\n
				return ByteUtils.stepBuffer(buffer, 5);
			} else {
				return decodeError(session, buffer);
			}
		} else {
			Boolean result = (Boolean) this.result;
			if (result) {
				// END\r\n
				return ByteUtils.stepBuffer(buffer, 5);
			} else {
				return decodeError(session, buffer);
			}
		}
	}

}
