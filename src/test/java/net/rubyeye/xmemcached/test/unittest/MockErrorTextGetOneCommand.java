package net.rubyeye.xmemcached.test.unittest;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;

public class MockErrorTextGetOneCommand extends TextGetOneCommand {

	private volatile boolean decoded;

	public MockErrorTextGetOneCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch) {
		super(key, keyBytes, cmdType, latch);

	}

	public void dispatch() {
		decoded = true;
		countDownLatch();
		decodeError();
	}

	public boolean isDecoded() {
		return decoded;
	}

	public void setDecoded(boolean decoded) {
		this.decoded = decoded;
	}

}
