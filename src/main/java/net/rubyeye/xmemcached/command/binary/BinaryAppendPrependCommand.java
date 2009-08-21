package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Binary protocol for append,prepend
 * 
 * @author dennis
 * 
 */
@SuppressWarnings("unchecked")
public class BinaryAppendPrependCommand extends BaseBinaryCommand {

	public BinaryAppendPrependCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, int exp, long cas,
			Object value, boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
		switch (cmdType) {
		case APPEND:
			this.opCode = noreply ? OpCode.APPEND_QUIETLY : OpCode.APPEND;
			break;
		case PREPEND:
			this.opCode = noreply ? OpCode.PREPEND_QUIETLY : OpCode.PREPEND;
			break;
		default:
			throw new UnknownCommandException(
					"Not a append or prepend command:" + cmdType.name());
		}
	}

	@Override
	protected void fillExtras(CachedData data) {
		// no extras
	}

	@Override
	protected byte getExtrasLength() {
		return 0;
	}

}
