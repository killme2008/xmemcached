package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.StoreCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextStoreCommand extends StoreCommand {
	private String cmdStr;

	@SuppressWarnings("unchecked")
	public TextStoreCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			Transcoder transcoder, String cmdStr) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, transcoder);
		this.cmdStr = cmdStr;
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("STORED")) {
				setResult(Boolean.TRUE);
				countDownLatch();
				return true;
			} else if (line.equals("NOT_STORED")) {
				setResult(Boolean.FALSE);
				countDownLatch();
				return true;
			} else
				decodeError();
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void encode(BufferAllocator bufferAllocator) {
		final CachedData data = transcoder.encode(value);
		byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlags()));
		byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
		byte[] dataLenBytes = ByteUtils.getBytes(String
				.valueOf(data.getData().length));
		byte[] casBytes = ByteUtils.getBytes(String.valueOf(cas));
		int size = cmdStr.length() + 1 + keyBytes.length + 1 + flagBytes.length
				+ 1 + expBytes.length + 1 + data.getData().length + 2
				* Constants.CRLF.length + dataLenBytes.length;
		if (this.commandType == CommandType.CAS) {
			size += 1 + casBytes.length;
		}
		this.ioBuffer = bufferAllocator.allocate(size);
		if (this.commandType == CommandType.CAS) {
			ByteUtils.setArguments(this.ioBuffer, cmdStr, keyBytes, flagBytes,
					expBytes, dataLenBytes, casBytes);
		} else {
			ByteUtils.setArguments(this.ioBuffer, cmdStr, keyBytes, flagBytes,
					expBytes, dataLenBytes);
		}
		ByteUtils.setArguments(this.ioBuffer, data.getData());
		this.ioBuffer.flip();
	}

}
