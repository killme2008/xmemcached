package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

public class BinaryDeleteCommand extends BaseBinaryCommand {

	public BinaryDeleteCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, boolean noreply) {
		super(key, keyBytes, cmdType, latch, 0, 0, null, noreply, null);
		this.opCode = noreply?OpCode.DELETE_QUIETLY:OpCode.DELETE;
	}

	@Override
	protected void fillExtras(CachedData data) {
		// must not have extras
	}

	@Override
	protected byte getExtrasLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
	}

}
