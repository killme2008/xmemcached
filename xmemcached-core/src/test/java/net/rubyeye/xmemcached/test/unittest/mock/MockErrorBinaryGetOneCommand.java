package net.rubyeye.xmemcached.test.unittest.mock;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.OpCode;

public class MockErrorBinaryGetOneCommand extends BinaryGetCommand implements
		MockErrorCommand {

	private boolean decode;

	public MockErrorBinaryGetOneCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, OpCode opCode,
			boolean noreply) {
		super(key, keyBytes, cmdType, latch, opCode, noreply);
		// TODO Auto-generated constructor stub
	}

	
	
	@Override
	protected boolean finish() {
		this.decode=true;
		super.finish();
		decodeError();
		return true;
	}


	
	public boolean isDecoded() {
		return this.decode;
	}

}
