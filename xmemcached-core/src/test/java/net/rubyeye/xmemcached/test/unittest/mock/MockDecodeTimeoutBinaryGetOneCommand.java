package net.rubyeye.xmemcached.test.unittest.mock;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class MockDecodeTimeoutBinaryGetOneCommand extends BinaryGetCommand {
	private long sleepTime;

	public MockDecodeTimeoutBinaryGetOneCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, OpCode opCode,
			boolean noreply, long sleepTime) {
		super(key, keyBytes, cmdType, latch, opCode, noreply);
		this.sleepTime=sleepTime;
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer){
		// TODO Auto-generated method stub
		try {
			Thread.sleep(this.sleepTime);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return super.decode(session, buffer);

	}

}
