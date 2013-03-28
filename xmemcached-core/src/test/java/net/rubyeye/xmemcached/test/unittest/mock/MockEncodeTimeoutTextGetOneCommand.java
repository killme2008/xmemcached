package net.rubyeye.xmemcached.test.unittest.mock;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;

public class MockEncodeTimeoutTextGetOneCommand extends TextGetOneCommand {

	private long sleepTime;

	public MockEncodeTimeoutTextGetOneCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, long sleepTime) {
		super(key, keyBytes, cmdType, latch);
		this.sleepTime = sleepTime;
	}

	public void encode() {
		// Sleep,then encode timeout
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.encode();
	}

}
