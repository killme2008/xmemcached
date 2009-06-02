package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public abstract class StatsCommand extends Command {
	public static final ByteBuffer STATS = ByteBuffer.wrap("stats\r\n"
			.getBytes());
	protected InetSocketAddress server;
	// TODO provide stats item
	protected String item;

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public StatsCommand(InetSocketAddress server, final CountDownLatch latch) {
		super("stats", (byte[]) null, latch);
		this.commandType = CommandType.STATS;
		this.server = server;
	}

	public final InetSocketAddress getServer() {
		return server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}
}
