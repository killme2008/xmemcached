package net.rubyeye.xmemcached.command;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public abstract class VersionCommand extends Command {
	public static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n"
			.getBytes());

	public VersionCommand(final CountDownLatch latch) {
		super("version",(byte[])null,latch);
		this.commandType = CommandType.VERSION;
	}

}
