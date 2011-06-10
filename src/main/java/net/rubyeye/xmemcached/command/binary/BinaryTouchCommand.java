package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

/**
 * Binary touch command
 * 
 * @author dennis
 * @since 1.3.3
 */
public class BinaryTouchCommand extends BaseBinaryCommand {

	public BinaryTouchCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, boolean noreply) {
		super(key, keyBytes, cmdType, latch, exp, 0, null, noreply, null);
		this.opCode = OpCode.TOUCH;

	}

	@Override
	protected void fillExtras(CachedData data) {
		this.ioBuffer.putInt(this.expTime);
	}

	@Override
	protected void fillValue(CachedData data) {
		// Must not have value
	}

	@Override
	protected byte getExtrasLength() {
		return 4;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

}
