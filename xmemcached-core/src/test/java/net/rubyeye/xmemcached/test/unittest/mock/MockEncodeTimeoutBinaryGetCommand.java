package net.rubyeye.xmemcached.test.unittest.mock;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.OpCode;

public class MockEncodeTimeoutBinaryGetCommand extends BinaryGetCommand {
	private long sleepTime;

	public MockEncodeTimeoutBinaryGetCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, OpCode opCode,
			boolean noreply, long sleepTime) {
		super(key, keyBytes, cmdType, latch, opCode, noreply);
		this.sleepTime=sleepTime;
	}

	@Override
	public void encode() {
		try {
			Thread.sleep(this.sleepTime);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		super.encode();
	}

}
