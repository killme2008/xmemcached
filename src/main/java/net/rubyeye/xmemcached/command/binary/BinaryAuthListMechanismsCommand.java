package net.rubyeye.xmemcached.command.binary;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

/**
 * List auth mechanisms command
 * 
 * @author dennis
 * 
 */
public class BinaryAuthListMechanismsCommand extends BaseBinaryCommand {

	public BinaryAuthListMechanismsCommand(CountDownLatch latch) {
		super(null, null, CommandType.AUTH_LIST, latch, 0, 0, null, false, null);
		this.opCode = OpCode.AUTH_LIST_MECHANISMS;
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

	@Override
	protected void fillExtras(CachedData data) {
		// must not have extras
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
	}

	@Override
	protected byte getExtrasLength() {
		return 0;
	}

	@Override
	protected void fillKey() {
		// must not have key
	}

	@Override
	protected int getKeyLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}
}
