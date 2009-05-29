package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.buffer.SimpleIoBuffer;
import net.rubyeye.xmemcached.command.AbstractCommand;
import net.rubyeye.xmemcached.command.BooleanResultCommand;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class FlushAllCommand extends BooleanResultCommand {
	public FlushAllCommand() {
		super();
		this.key = CommandFactory.FLUSH_ALL.array();
		this.commandType=CommandType.FLUSH_ALL;
	}

	@Override
	public boolean decode(ByteBuffer buffer, MemcachedTCPSession session) {
		if (session.getCurrentLine().equals("OK"))
			return notifyBoolean(session, Boolean.TRUE);
		else {
			session.close();
			return false;
		}
	}

	@Override
	public void encode(BufferAllocator allocator) {
		this.ioBuffer = new SimpleIoBuffer(CommandFactory.FLUSH_ALL.slice());
	}

}
