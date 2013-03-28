package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

/**
 * Binary verbosity command
 * 
 * @author dennis
 * @since 1.3.3
 * 
 */
public class BinaryVerbosityCommand extends BaseBinaryCommand {

	private int verbosity;

	public BinaryVerbosityCommand(CountDownLatch latch, int verbosity,
			boolean noreply) {
		super(null, null, CommandType.VERBOSITY, latch, 0, 0, null, noreply,
				null);
		this.opCode = OpCode.VERBOSITY;
	}

	@Override
	protected void fillExtras(CachedData data) {
		this.ioBuffer.putInt(verbosity);
	}

	protected void fillKey() {
		// MUST NOT have key.
	}

	@Override
	protected byte getExtrasLength() {
		// Total 4 bytes
		return 4;
	}

	@Override
	protected int getKeyLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

	protected void fillValue(CachedData data) {
		// MUST NOT have value.
	}

}
