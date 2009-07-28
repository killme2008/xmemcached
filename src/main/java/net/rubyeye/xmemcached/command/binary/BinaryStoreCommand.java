package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Base binary protocol implementation
 * 
 * @author dennis
 * 
 */
@SuppressWarnings("unchecked")
public class BinaryStoreCommand extends BaseBinaryCommand {

	public BinaryStoreCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
		switch (cmdType) {
		case SET:
			this.opCode = OpCode.SET;
			break;
		case REPLACE:
			this.opCode = OpCode.REPLACE;
			break;
		case ADD:
			this.opCode = OpCode.ADD;
			break;

		}
	}
}
