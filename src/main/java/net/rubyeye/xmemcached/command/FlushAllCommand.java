package net.rubyeye.xmemcached.command;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public abstract class FlushAllCommand extends Command {
	public static final ByteBuffer FLUSH_ALL = ByteBuffer.wrap("flush_all\r\n"
			.getBytes());

	private int delay;

	public final int getDelay() {
		return delay;
	}

	public FlushAllCommand(final CountDownLatch latch, int delay,
			boolean noreply) {
		super("flush_all", (byte[]) null, latch);
		this.commandType = CommandType.FLUSH_ALL;
		this.delay = delay;
		this.noreply = noreply;
	}
}
