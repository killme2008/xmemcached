package net.rubyeye.xmemcached.command;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public abstract class FlushAllCommand extends Command {
	public static final ByteBuffer FLUSH_ALL = ByteBuffer.wrap("flush_all\r\n"
			.getBytes());

	public FlushAllCommand(final CountDownLatch latch) {
		super("flush_all", (byte[]) null, latch);
		this.commandType = CommandType.FLUSH_ALL;
	}
}
