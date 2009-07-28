package net.rubyeye.xmemcached.command.text;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.MergeCommandsAware;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

public abstract class TextGetCommand extends Command implements MergeCommandsAware{
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't', 's' };
	protected Map<String, CachedData> returnValues;
	private String currentReturnKey;
	private int offset;
	/**
	 *When MemcachedClient merge get commands,those commans which have the same
	 * key will be merged into one get command.The result command's
	 * assocCommands contains all these commands with the same key.
	 */
	private List<Command> assocCommands;

	private Map<Object, Command> mergeCommands;

	public final Map<Object, Command> getMergeCommands() {
		return this.mergeCommands;
	}

	public final void setMergeCommands(Map<Object, Command> mergeCommands) {
		this.mergeCommands = mergeCommands;
	}

	public final List<Command> getAssocCommands() {
		return this.assocCommands;
	}

	public final void setAssocCommands(List<Command> assocCommands) {
		this.assocCommands = assocCommands;
	}

	public TextGetCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch) {
		super(key, keyBytes, cmdType, latch);
		this.returnValues = new HashMap<String, CachedData>();
	}

	private ParseStatus parseStatus = ParseStatus.NULL;

	public static enum ParseStatus {
		NULL, VALUE, KEY, FLAG, DATA_LEN, DATA_LEN_DONE, CAS, DATA, END
	}

	protected boolean wasFirst = true;

	public ParseStatus getParseStatus() {
		return this.parseStatus;
	}

	public void setParseStatus(ParseStatus parseStatus) {
		this.parseStatus = parseStatus;
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		while (true) {
			if (buffer == null || !buffer.hasRemaining()) {
				return false;
			}
			switch (this.parseStatus) {

			case NULL:
				byte first = buffer.get(buffer.position());
				if (first == 'E') {
					this.parseStatus = ParseStatus.END;
					dispatch();
					this.currentReturnKey = null;
					continue;
				} else if (first == 'V') {
					this.parseStatus = ParseStatus.VALUE;
					this.wasFirst = false;
					continue;
				} else {
					return decodeError(session, buffer);
				}
			case END:
				// END\r\n
				return ByteUtils.stepBuffer(buffer, 5);
			case VALUE:
				// VALUE[SPACE]
				if (ByteUtils.stepBuffer(buffer, 6)) {
					this.parseStatus = ParseStatus.KEY;
					continue;
				} else {
					return false;
				}
			case KEY:
				String item = getItem(buffer, ' ');
				if (item == null) {
					return false;
				} else {
					this.currentReturnKey = item;
					this.returnValues.put(this.currentReturnKey,
							new CachedData());
					this.parseStatus = ParseStatus.FLAG;
					continue;
				}
			case FLAG:
				item = getItem(buffer, ' ');
				if (item == null) {
					return false;
				} else {
					final CachedData cachedData = this.returnValues
							.get(this.currentReturnKey);
					cachedData.setFlag(Integer.parseInt(item));
					this.parseStatus = ParseStatus.DATA_LEN;
					continue;
				}
			case DATA_LEN:
				item = getItem(buffer, '\r', ' ');
				if (item == null) {
					return false;
				} else {
					final CachedData cachedData = this.returnValues
							.get(this.currentReturnKey);
					cachedData.setCapacity(Integer.parseInt(item));
					assert (cachedData.getCapacity() >= 0);
					cachedData.setData(new byte[cachedData.getCapacity()]);
					this.parseStatus = ParseStatus.DATA_LEN_DONE;
					continue;
				}
			case DATA_LEN_DONE:
				if (buffer.remaining() < 1) {
					return false;
				} else {
					first = buffer.get(buffer.position());
					if (first == '\n') {
						// skip '\n'
						buffer.position(buffer.position() + 1);
						this.parseStatus = ParseStatus.DATA;
						continue;
					} else {
						this.parseStatus = ParseStatus.CAS;
						continue;
					}
				}
			case CAS:
				// has cas value
				item = getItem(buffer, '\r');
				if (item == null) {
					return false;
				} else {
					final CachedData cachedData = this.returnValues
							.get(this.currentReturnKey);
					cachedData.setCas(Long.parseLong(item));
					this.parseStatus = ParseStatus.DATA;
					// skip '\n'
					buffer.position(buffer.position() + 1);
					continue;
				}
			case DATA:
				final CachedData value = this.returnValues
						.get(this.currentReturnKey);
				int remaining = buffer.remaining();
				int remainingCapacity = value.remainingCapacity();
				assert (remainingCapacity >= 0);
				// 不够数据，返回
				if (remaining < remainingCapacity + 2) {
					int length = remaining > remainingCapacity ? remainingCapacity
							: remaining;
					value.fillData(buffer, length);
					return false;
				} else if (remainingCapacity > 0) {
					value.fillData(buffer, remainingCapacity);
				}
				assert (value.remainingCapacity() == 0);
				buffer
						.position(buffer.position()
								+ ByteUtils.SPLIT.remaining());

				Map<Object, Command> mergetCommands = getMergeCommands();
				if (mergetCommands != null) {
					final TextGetCommand command = (TextGetCommand) mergetCommands
							.remove(this.currentReturnKey);
					if (command != null) {
						command.setResult(value);
						command.countDownLatch();
						this.mergeCount--;
						if (command.getAssocCommands() != null) {
							for (Command assocCommand : command
									.getAssocCommands()) {
								assocCommand.setResult(value);
								assocCommand.countDownLatch();
								this.mergeCount--;
							}
						}

					}
				}
				this.currentReturnKey = null;
				this.parseStatus = ParseStatus.NULL;
				continue;
			default:
				return decodeError(session, buffer);
			}

		}
	}

	private String getItem(ByteBuffer buffer, char token, char... others) {
		int pos = buffer.position() + this.offset;
		final int limit = buffer.limit();
		for (; pos < limit; pos++) {
			final byte b = buffer.get(pos);
			if (b == token || isIn(b, others)) {
				byte[] keyBytes = new byte[pos - buffer.position()];
				buffer.get(keyBytes);
				this.offset = 0;
				assert (pos == buffer.position());
				// skip token
				buffer.position(pos + 1);
				return getString(keyBytes);
			}
		}
		this.offset = pos - buffer.position();
		return null;
	}

	private boolean isIn(byte b, char[] others) {
		for (int i = 0; i < others.length; i++) {
			if (b == others[i]) {
				return true;
			}
		}
		return false;
	}

	private String getString(byte[] keyBytes) {
		try {
			return new String(keyBytes, "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public final Map<String, CachedData> getReturnValues() {
		return this.returnValues;
	}

	public final void setReturnValues(Map<String, CachedData> returnValues) {
		this.returnValues = returnValues;
	}

	public abstract void dispatch();

	@Override
	public void encode(BufferAllocator bufferAllocator) {
		byte[] cmdBytes = this.commandType == CommandType.GET_ONE
				|| this.commandType == CommandType.GET_MANY ? GET : GETS;
		this.ioBuffer = bufferAllocator.allocate(cmdBytes.length
				+ Constants.CRLF.length + 1 + this.keyBytes.length);
		ByteUtils.setArguments(this.ioBuffer, cmdBytes, this.keyBytes);
		this.ioBuffer.flip();
	}

}
