package net.rubyeye.xmemcached.command.binary;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

/**
 * Auth step command
 * 
 * @author dennis
 * 
 */
public class BinaryAuthStepCommand extends BaseBinaryCommand {

	public BinaryAuthStepCommand(String mechanism, byte[] keyBytes,
			CountDownLatch latch, byte[] authData) {
		super(mechanism, keyBytes, CommandType.AUTH_STEP, latch, 0, 0,
				authData, false, null);
		this.opCode = OpCode.AUTH_STEP;
	}

	@Override
	protected void fillExtras(CachedData data) {
		// must not have extras
	}

	@Override
	protected void fillValue(CachedData data) {
		if (this.value != null)
			this.ioBuffer.put((byte[]) this.value);
	}

	@Override
	protected int getValueLength(CachedData data) {
		if (this.value == null)
			return 0;
		else
			return ((byte[]) this.value).length;
	}

	@Override
	protected byte getExtrasLength() {
		return (byte) 0;
	}

	@Override
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		int valueLength = bodyLength - keyLength - extrasLength;
		if (buffer.remaining() < valueLength) {
			return false;
		}
		byte[] bytes = new byte[valueLength];
		buffer.get(bytes);
		setResult(new String(bytes));
		countDownLatch();
		return true;
	}

}
