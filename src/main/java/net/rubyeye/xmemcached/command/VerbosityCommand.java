package net.rubyeye.xmemcached.command;

import java.util.concurrent.CountDownLatch;

public abstract class VerbosityCommand extends Command {

	protected int level;

	public final int getLevel() {
		return level;
	}

	public final void setLevel(int logLevel) {
		this.level = logLevel;
	}

	public VerbosityCommand(CountDownLatch latch,
			int level, boolean noreply) {
		super(CommandType.VERBOSITY, latch);
		this.level = level;
		this.noreply = noreply;
	}

}
