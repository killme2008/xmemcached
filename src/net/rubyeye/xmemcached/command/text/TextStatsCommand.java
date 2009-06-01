package net.rubyeye.xmemcached.command.text;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.text.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.StatsCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public class TextStatsCommand extends StatsCommand {
	public TextStatsCommand(InetSocketAddress server, CountDownLatch latch) {
		super(server, latch);
		this.result = new HashMap<String, String>();

	}

	private boolean done = false;

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = MemcachedTextDecoder.nextLine(session, buffer);
		if (line != null) {
			if (line.startsWith("STAT")) {
				while ((line = session.getCurrentLine()) != null) {
					if (line.equals("END")) { // 到消息结尾
						return done(session);
					}
					String[] items = line.split(" ");
					((Map<String, String>) getResult()).put(items[1], items[2]);
					session.removeCurrentLine();
					MemcachedTextDecoder.nextLine(session, buffer);
				}
				return false;
			} else if (!done && line.equals("END")) {
				return done(session);
			} else
				decodeError();
		}
		return false;
	}

	private boolean done(MemcachedTCPSession session) {
		done = true;
		session.resetStatus();
		countDownLatch();
		return true;
	}

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		this.ioBuffer = bufferAllocator.wrap(STATS.slice());
	}

}
