package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextCASCommand extends TextStoreCommand {

	@SuppressWarnings("unchecked")
	public TextCASCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
	}

	private FailStatus failStatus;

	static enum FailStatus {
		NOT_FOUND, EXISTS
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {

		if (buffer == null || !buffer.hasRemaining())
			return false;
		if (result == null) {
			byte first = buffer.get(buffer.position());
			if (first == 'S') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// STORED\r\n
				return ByteUtils.stepBuffer(buffer, 8);
			} else if (first == 'N') {
				setResult(Boolean.FALSE);
				countDownLatch();
				failStatus = FailStatus.NOT_FOUND;
				// NOT_FOUND\r\n
				return ByteUtils.stepBuffer(buffer, 11);
			} else if (first == 'E') {
				setResult(Boolean.FALSE);
				countDownLatch();
				failStatus = FailStatus.EXISTS;
				// EXISTS\r\n
				return ByteUtils.stepBuffer(buffer, 8);
			} else
				return decodeError(session, buffer);
		} else {
			Boolean result = (Boolean) this.result;
			if (result) {
				return ByteUtils.stepBuffer(buffer, 8);
			} else {
				switch (this.failStatus) {
				case NOT_FOUND:
					return ByteUtils.stepBuffer(buffer, 11);
				case EXISTS:
					return ByteUtils.stepBuffer(buffer, 8);
				default:
					return decodeError(session, buffer);
				}
			}
		}
	}
}
