package net.rubyeye.xmemcached.command.text;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextStatsCommand extends Command implements ServerAddressAware {
	public static final ByteBuffer STATS = ByteBuffer.wrap("stats\r\n"
			.getBytes());
	private InetSocketAddress server;
	private String itemName;


	public String getItemName() {
		return this.itemName;
	}

	public final InetSocketAddress getServer() {
		return this.server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}

	public void setItemName(String item) {
		this.itemName = item;
	}
	public TextStatsCommand(InetSocketAddress server, CountDownLatch latch,
			String itemName) {
		super("stats", (byte[]) null, latch);
		this.commandType = CommandType.STATS;
		this.server = server;
		this.itemName = itemName;
		this.result = new HashMap<String, String>();

	}

	@Override
	@SuppressWarnings("unchecked")
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = null;
		while ((line = ByteUtils.nextLine(buffer)) != null) {
			if (line.equals("END")) { // at the end
				return done(session);
			} else if (line.startsWith("STAT")) {
				String[] items = line.split(" ");
				((Map<String, String>) getResult()).put(items[1], items[2]);
			} else {
				return decodeError(line);
			}
		}
		return false;
	}

	private final boolean done(MemcachedTCPSession session) {
		countDownLatch();
		return true;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		if (this.itemName == null) {
			this.ioBuffer = bufferAllocator.wrap(STATS.slice());
		} else {
			this.ioBuffer = bufferAllocator
					.allocate(5 + this.itemName.length() + 3);
			ByteUtils.setArguments(this.ioBuffer, "stats", this.itemName);
			this.ioBuffer.flip();
		}
	}

}
