package net.rubyeye.xmemcached.command.binary;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.AssocCommandAware;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * Implements get/getq,getk/getkq protocol
 * 
 * @author dennis
 * 
 */
public class BinaryGetCommand extends BaseBinaryCommand implements
		AssocCommandAware {
	private String responseKey;
	private CachedData responseValue;
	private List<Command> assocCommands;

	public final String getResponseKey() {
		return this.responseKey;
	}

	public final void setResponseKey(String responseKey) {
		this.responseKey = responseKey;
	}

	public BinaryGetCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, OpCode opCode, boolean noreply) {
		super(key, keyBytes, cmdType, latch, 0, 0, null, noreply, null);
		this.opCode = opCode;
		this.responseValue = new CachedData();
	}

	public final List<Command> getAssocCommands() {
		return this.assocCommands;
	}

	public final void setAssocCommands(List<Command> assocCommands) {
		this.assocCommands = assocCommands;
	}

	@Override
	protected boolean finish() {
		countDownLatch();
		return true;
	}

	@Override
	protected boolean readKey(ByteBuffer buffer, int keyLength) {
		if (buffer.remaining() < keyLength) {
			return false;
		}
		if (keyLength > 0) {
			byte[] bytes = new byte[keyLength];
			buffer.get(bytes);
			this.responseKey = ByteUtils.getString(bytes);
		}
		return true;
	}

	@Override
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		if (this.responseStatus == ResponseStatus.NO_ERROR) {
			int valueLength = bodyLength - keyLength - extrasLength;
			if (valueLength >= 0 && this.responseValue.getCapacity() < 0) {
				this.responseValue.setCapacity(valueLength);
				this.responseValue.setData(new byte[valueLength]);
			}
			int remainingCapacity = this.responseValue.remainingCapacity();
			int remaining = buffer.remaining();
			if (remaining < remainingCapacity) {
				int length = remaining > remainingCapacity ? remainingCapacity
						: remaining;
				this.responseValue.fillData(buffer, length);
				return false;
			} else if (remainingCapacity > 0) {
				this.responseValue.fillData(buffer, remainingCapacity);
			}
			setResult(this.responseValue);
			return true;
		} else {
			return ByteUtils.stepBuffer(buffer, bodyLength - keyLength
					- extrasLength);
		}
	}

	@Override
	protected boolean readExtras(ByteBuffer buffer, int extrasLength) {
		if (buffer.remaining() < extrasLength) {
			return false;
		}
		if (extrasLength > 0) {
			// read flag
			int flag = buffer.getInt();
			this.responseValue.setFlag(flag);
		}
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
	protected int getValueLength(CachedData data) {
		return 0;
	}

	@Override
	protected long readCAS(ByteBuffer buffer) {
		long cas = buffer.getLong();
		this.responseValue.setCas(cas);
		return cas;
	}

}
