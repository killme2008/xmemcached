package net.rubyeye.xmemcached.command;

import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

public abstract class BooleanResultCommand extends AbstractCommand {
	protected final boolean notifyBoolean(MemcachedTCPSession session,
			Boolean result) {
		this.result = result;
		this.countDownLatch();
		session.resetStatus();
		return true;
	}

}
