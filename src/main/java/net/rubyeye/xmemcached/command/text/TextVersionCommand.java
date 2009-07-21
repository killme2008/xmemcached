package net.rubyeye.xmemcached.command.text;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextVersionCommand extends Command implements ServerAddressAware {
	public InetSocketAddress server;

	public final InetSocketAddress getServer() {
		return this.server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}

	public TextVersionCommand(InetSocketAddress server,
			final CountDownLatch latch) {
		super("version", (byte[]) null, latch);
		this.commandType = CommandType.VERSION;
		this.server = server;
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = ByteUtils.nextLine(buffer);
		if (line != null) {
			if (line.startsWith("VERSION")) {
				String[] items = line.split(" ");
				setResult(items.length > 1 ? items[1] : "unknown version");
				countDownLatch();
				return true;
			} else {
				return decodeError(line);
			}
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		this.ioBuffer = bufferAllocator.wrap(VERSION.slice());
	}

}
