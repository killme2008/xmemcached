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
public class BinaryCASCommand extends BaseBinaryCommand {

	public BinaryCASCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
		switch (cmdType) {
		case CAS:
			this.opCode = noreply ? OpCode.SET_QUIETLY : OpCode.SET;
			break;
		default:
			throw new IllegalArgumentException("Unknow cas command type:"
					+ cmdType);
		}
		
		
	}
	@Override
	protected void fillCAS() {
		this.ioBuffer.putLong(this.cas);
	}
	
	
}
