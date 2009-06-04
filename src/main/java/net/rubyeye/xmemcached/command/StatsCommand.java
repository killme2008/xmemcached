package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public abstract class StatsCommand extends Command {
	public static final ByteBuffer STATS = ByteBuffer.wrap("stats\r\n"
			.getBytes());
	protected InetSocketAddress server;
	protected String itemName;

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String item) {
		this.itemName = item;
	}

	public StatsCommand(InetSocketAddress server, final CountDownLatch latch,
			String itemName) {
		super("stats", (byte[]) null, latch);
		this.commandType = CommandType.STATS;
		this.server = server;
		this.itemName = itemName;
	}

	public final InetSocketAddress getServer() {
		return server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}
}
