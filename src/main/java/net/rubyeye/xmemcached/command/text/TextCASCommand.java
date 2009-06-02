package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.codec.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class TextCASCommand extends TextStoreCommand {

	@SuppressWarnings("unchecked")
	public TextCASCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			Transcoder transcoder, String cmdStr) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, transcoder,
				cmdStr);
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("STORED")) {
				setResult(Boolean.TRUE);
				countDownLatch();
				return true;
			} else if (line.equals("EXISTS") || line.equals("NOT_FOUND")) {
				setResult(Boolean.FALSE);
				countDownLatch();
				return true;
			} else
				decodeError();
		}
		return false;
	}

}
