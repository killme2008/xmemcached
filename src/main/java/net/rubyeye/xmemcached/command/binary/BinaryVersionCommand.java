package net.rubyeye.xmemcached.command.binary;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.transcoders.CachedData;

public class BinaryVersionCommand extends BaseBinaryCommand implements
		ServerAddressAware {
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
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		int valueLength = bodyLength - keyLength - extrasLength;
		if (buffer.remaining() < valueLength) {
			return false;
		}
		byte[] bytes = new byte[valueLength];
		buffer.get(bytes);
		setResult(new String(bytes));
		countDownLatch();
		return true;
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
