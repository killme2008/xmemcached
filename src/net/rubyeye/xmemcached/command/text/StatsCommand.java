package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleIoBuffer;
import net.rubyeye.xmemcached.command.AbstractCommand;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

/**
 * Stats command
 *
 * @author dennis
 *
 */
public class StatsCommand extends AbstractCommand {

	public StatsCommand() {
		super();
		this.result = new HashMap<String, String>();
		this.commandType=CommandType.STATS;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean decode(ByteBuffer buffer, MemcachedTCPSession session) {
		String line = null;
		while ((line = session.getCurrentLine()) != null) {
			if (line.equals("END")) { // 到消息结尾
				session.resetStatus();
				this.countDownLatch();
				return true;
			}
			String[] items = line.split(" ");
			((Map<String, String>) this.result).put(items[1], items[2]);
			session.setCurrentLine(null);
			MemcachedHandler.nextLine(session, buffer);
		}
		return false;
	}

	@Override
	public void encode(BufferAllocator allocator) {
		this.ioBuffer = new SimpleIoBuffer(CommandFactory.STATS.slice());
	}

}
