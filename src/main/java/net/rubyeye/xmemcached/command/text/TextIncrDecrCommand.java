package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.IncrDecrCommand;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextIncrDecrCommand extends IncrDecrCommand {

	public static final byte[] INCR = { 'i', 'n', 'c', 'r' };
	public static final byte[] DECR = { 'd', 'e', 'c', 'r' };

	public TextIncrDecrCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, int increment) {
		super(key, keyBytes, cmdType, latch, increment);

	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.equals("NOT_FOUND")) {
				setException(new MemcachedException(
						"The key's value is not found for increament or decrement"));
				countDownLatch();
				return true;
			} else {
				// TODO add statistics
				// statistics(executingCmd.getCommandType());
				setResult(Integer.parseInt(line));
				countDownLatch();
				return true;
			}
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		byte[] numBytes = ByteUtils.getBytes(String
				.valueOf(this.getIncrement()));
		byte[] cmdBytes = this.commandType == CommandType.INCR ? INCR : DECR;
		this.ioBuffer = bufferAllocator.allocate(cmdBytes.length + 2
				+ key.length() + numBytes.length
				+ Constants.CRLF.length);
		ByteUtils.setArguments(this.ioBuffer, cmdBytes, keyBytes, numBytes);
		this.ioBuffer.flip();

	}

}
