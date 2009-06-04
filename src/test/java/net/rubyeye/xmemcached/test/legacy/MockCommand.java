package net.rubyeye.xmemcached.test.legacy;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class MockCommand extends Command {

	public MockCommand() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MockCommand(CommandType cmdType, CountDownLatch latch) {
		super(cmdType, latch);
		// TODO Auto-generated constructor stub
	}

	public MockCommand(CommandType cmdType) {
		super(cmdType);
		// TODO Auto-generated constructor stub
	}

	public MockCommand(String key, byte[] keyBytes) {
		super(key, keyBytes,null);
		// TODO Auto-generated constructor stub
	}

	public MockCommand(String key, CommandType commandType, CountDownLatch latch) {
		super(key, commandType, latch);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		// TODO Auto-generated method stub

	}

}
