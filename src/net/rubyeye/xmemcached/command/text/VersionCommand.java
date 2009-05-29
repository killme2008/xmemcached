package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleIoBuffer;
import net.rubyeye.xmemcached.command.AbstractCommand;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

/**
 * Version command for getting memcached's version
 *
 * @author dennis
 *
 */
public class VersionCommand extends AbstractCommand {

	public VersionCommand() {
		super();
		this.key = CommandFactory.VERSION.array();
		this.commandType = CommandType.VERSION;
	}

	@Override
	public boolean decode(ByteBuffer buffer, MemcachedTCPSession session) {
		if (session.getCurrentLine().startsWith("VERSION")) {
			String[] items = session.getCurrentLine().split(" ");
			final String version = items.length > 1 ? items[1]
					: "unknown version";
			this.result = version;
			this.countDownLatch();
			session.resetStatus();
			return true;
		} else {
			session.close();
			return false;
		}

	}

	@Override
	public void encode(BufferAllocator allocator) {
		this.ioBuffer = new SimpleIoBuffer(CommandFactory.VERSION.slice());
	}

}
