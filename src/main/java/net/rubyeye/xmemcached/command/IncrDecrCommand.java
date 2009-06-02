package net.rubyeye.xmemcached.command;

import java.util.concurrent.CountDownLatch;

public abstract class IncrDecrCommand extends Command {
	private int increment;

	
	public IncrDecrCommand(String key, byte[] keyBytes,CommandType cmdType, CountDownLatch latch,
			int increment) {
		super(key, keyBytes,cmdType, latch);
		this.increment = increment;
	}

	public final int getIncrement() {
		return increment;
	}

	public final void setIncrement(int increment) {
		this.increment = increment;
	}

}
