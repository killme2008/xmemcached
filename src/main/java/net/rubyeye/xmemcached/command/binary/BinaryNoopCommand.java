package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;

/**
 * Implement noop protocol
 * 
 * @author dennis
 * 
 */
public class BinaryNoopCommand extends BaseBinaryCommand {

	public BinaryNoopCommand(CountDownLatch latch) {
		super(null, null, CommandType.NOOP, latch, 0, 0, null, false, null);
		this.opCode = OpCode.NOOP;
	}

}
