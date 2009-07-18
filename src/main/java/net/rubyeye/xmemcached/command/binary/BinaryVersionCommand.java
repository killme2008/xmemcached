package net.rubyeye.xmemcached.command.binary;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.VersionCommand;
import net.rubyeye.xmemcached.transcoders.CachedData;

public class BinaryVersionCommand extends BaseBinaryCommand implements
		VersionCommand {
	public InetSocketAddress server;

	public final InetSocketAddress getServer() {
		return this.server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}

	public BinaryVersionCommand(final CountDownLatch latch,
			InetSocketAddress server) {
		super("version", null, CommandType.VERSION, latch, 0, 0, latch, false,
				null);
		this.commandType = CommandType.VERSION;
		this.server = server;
		this.opCode = OpCode.VERSION;
	}

	@Override
	protected void readValue(ByteBuffer buffer, int bodyLength, int keyLength,
			int extrasLength) {
		byte[] bytes = new byte[bodyLength - keyLength - extrasLength];
		buffer.get(bytes);
		setResult(new String(bytes));
		countDownLatch();
	}

	@Override
	protected void fillExtras(CachedData data) {
		// must not have extras
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
	}

	@Override
	protected byte getExtrasLength() {
		return 0;
	}

	@Override
	protected void fillKey() {
		// must not have key
	}

	@Override
	protected int getKeyLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

}
