package net.rubyeye.xmemcached.test.unittest.mock;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;

public class MockErrorTextGetOneCommand extends TextGetOneCommand implements MockErrorCommand{

	private volatile boolean decoded;

	public MockErrorTextGetOneCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch) {
		super(key, keyBytes, cmdType, latch);

	}

	@Override
	public void dispatch() {
		this.decoded = true;
		countDownLatch();
		decodeError();
	}

	public boolean isDecoded() {
		return this.decoded;
	}

	public void setDecoded(boolean decoded) {
		this.decoded = decoded;
	}

}
