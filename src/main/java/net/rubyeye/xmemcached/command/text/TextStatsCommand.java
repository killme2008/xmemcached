package net.rubyeye.xmemcached.command.text;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedDecoder;
import net.rubyeye.xmemcached.command.StatsCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class TextStatsCommand extends StatsCommand {
	public TextStatsCommand(InetSocketAddress server, CountDownLatch latch) {
		super(server, latch);
		this.result = new HashMap<String, String>();

	}
	private boolean wasFirst=true;

	@Override
	@SuppressWarnings("unchecked")
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = null;
		while ((line = MemcachedDecoder.nextLine(session, buffer)) != null) {
			if (line != null) {
				if (!wasFirst&&line.equals("END")) { // 到消息结尾
					return done(session);
				} else if (line.startsWith("STAT")) {
					wasFirst=false;
					String[] items = line.split(" ");
					((Map<String, String>) getResult()).put(items[1], items[2]);
				} else
					decodeError();
			} else
				return false;
		}
		return false;
	}

	private final boolean done(MemcachedTCPSession session) {
		countDownLatch();
		return true;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		this.ioBuffer = bufferAllocator.wrap(STATS.slice());
	}

}
