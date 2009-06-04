package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public abstract class VersionCommand extends Command {
	public static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n"
			.getBytes());
	protected InetSocketAddress server;

	public final InetSocketAddress getServer() {
		return server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}

	public VersionCommand(InetSocketAddress server, final CountDownLatch latch) {
		super("version", (byte[]) null, latch);
		this.commandType = CommandType.VERSION;
		this.server = server;
	}

}
