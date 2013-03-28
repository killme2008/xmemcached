package net.rubyeye.xmemcached.test.unittest.mock;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;

public class MockDecodeTimeoutTextGetOneCommand extends TextGetOneCommand {

	private long sleepTime;

	public MockDecodeTimeoutTextGetOneCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, long sleepTime) {
		super(key, keyBytes, cmdType, latch);
		this.sleepTime = sleepTime;
	}

	@Override
	public void dispatch() {
		// Sleep,then operation is timeout
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		super.dispatch();
	}

}
