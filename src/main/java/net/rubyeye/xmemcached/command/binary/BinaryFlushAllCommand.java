package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

public class BinaryFlushAllCommand extends BaseBinaryCommand {

	private int exptime;

	public BinaryFlushAllCommand(CountDownLatch latch, int exptime,
			boolean noreply) {
		super("flush_all", null, CommandType.FLUSH_ALL, latch, 0, 0, null,
				noreply, null);
		this.opCode=noreply?OpCode.FLUSH_QUIETLY:OpCode.FLUSH;
		this.expTime = exptime;
	}

	@Override
	protected void fillExtras(CachedData data) {
		if (this.expTime > 0) {
			this.ioBuffer.putInt(this.expTime);
		}
	}

	@Override
	protected byte getExtrasLength() {
		if (this.exptime > 0) {
			return 4;
		} else {
			return 0;
		}
	}

	@Override
	protected int getKeyLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

	@Override
	protected void fillKey() {
		// must not have key
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
	}

}
