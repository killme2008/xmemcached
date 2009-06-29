package net.rubyeye.xmemcached.command.text;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class TextGetCommand extends Command {
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't', 's' };
	protected Map<String, CachedData> returnValues;
	private String currentReturnKey;

	public static final Log log = LogFactory.getLog(TextGetCommand.class);

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
	// protected String currentLine;
	protected int currentPos = -1;

	public ParseStatus getParseStatus() {
		return parseStatus;
	}

	public void setParseStatus(ParseStatus parseStatus) {
		this.parseStatus = parseStatus;
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		while (true) {
			if (buffer == null || !buffer.hasRemaining())
				return false;
			switch (this.parseStatus) {

			case NULL:
				log.info(this.returnValues);
				byte first = buffer.get(buffer.position());
				if (first == 'E') {
					this.parseStatus = ParseStatus.END;
					log.info("end..dispatch");
					dispatch();
					this.currentReturnKey = null;
					continue;
				} else if (first == 'V') {
					log.info("Value parse");
					this.parseStatus = ParseStatus.VALUE;
					wasFirst = false;
					continue;
				} else
					return decodeError(session, buffer);
			case END:
				// END\r\n
				return ByteUtils.stepBuffer(buffer, 5);
			case VALUE:
				// VALUE[SPACE]
				if (ByteUtils.stepBuffer(buffer, 6)) {
					this.parseStatus = ParseStatus.KEY;
					this.currentPos = -1;
					continue;
				} else
					return false;
			case KEY:
				String item = getItem(buffer, ' ');
				if (item == null) {
					return false;
				} else {
					this.currentReturnKey = item;
					log.info("Key:" + this.currentReturnKey);
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
					cachedData.setFlags(Integer.parseInt(item));
					log.info("Flag:" + item);
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
					log.info("capacity:" + item);
					cachedData.setData(new byte[cachedData.getCapacity()]);
					this.parseStatus = ParseStatus.DATA_LEN_DONE;
					continue;
				}
			case DATA_LEN_DONE:
				if (buffer.remaining() < 1)
					return false;
				else {
					first = buffer.get(buffer.position());
					if (first == '\n') {
						// skip '\n'
						log.info("Enter parse data...");
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
				if (item == null)
					return false;
				else {
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
				log.info("remainingCapacity:" + remainingCapacity);
				// 不够数据，返回
				if (remaining < remainingCapacity + 2) {
					int length = remaining > remainingCapacity ? remainingCapacity
							: remaining;
					value.fillData(buffer, length);
					log.info("after1 fill remainingCapacity:"
							+ remainingCapacity);
					return false;
				} else if (remainingCapacity > 0) {
					value.fillData(buffer, remainingCapacity);
					log.info("after2 fill remainingCapacity:"
							+ value.remainingCapacity());
					if (value.remainingCapacity() > 0)
						return false;
				}
				buffer
						.position(buffer.position()
								+ ByteUtils.SPLIT.remaining());

				Map<Object, Command> mergetCommands = getMergeCommands();
				if (mergetCommands != null) {

					final Command command = mergetCommands
							.remove(this.currentReturnKey);
					if (command != null) {
						log.info("Count down:" + command.getKey());
						command.setResult(value);
						command.countDownLatch();
						this.mergeCount--;
					}
				}
				log.info("Parsed one value,enter next...");
				this.currentReturnKey = null;
				this.parseStatus = ParseStatus.NULL;
				this.currentPos = -1;
				continue;
			default:
				return decodeError(session, buffer);
			}

		}
		// while (true) {
		// if (currentLine == null)
		// currentLine = ByteUtils.nextLine(buffer);
		// if (currentLine != null) {
		// if (currentLine.equals("END")) {
		// dispatch();
		// this.currentReturnKey = null;
		// this.currentLine = null;
		// return true;
		// } else if (currentLine.startsWith("VALUE")) {
		// wasFirst = false;
		// if (currentReturnKey == null) {
		// StringTokenizer stringTokenizer = new StringTokenizer(
		// currentLine, " ");
		// stringTokenizer.nextToken();
		// this.currentReturnKey = stringTokenizer.nextToken();
		//
		// int flag = Integer
		// .parseInt(stringTokenizer.nextToken());
		// int dataLen = Integer.parseInt(stringTokenizer
		// .nextToken());
		// // maybe gets,it have cas value
		// CachedData value = new CachedData(flag,
		// new byte[dataLen], dataLen, -1);
		// value.setSize(0); // current size is zero
		// if (stringTokenizer.hasMoreTokens()) {
		// value.setCas(Long.parseLong(stringTokenizer
		// .nextToken()));
		// }
		// this.returnValues.put(this.currentReturnKey, value);
		// }
		//
		// CachedData value = this.returnValues
		// .get(this.currentReturnKey);
		// int remaining = buffer.remaining();
		// int remainingCapacity = value.getCapacity()
		// - value.getSize();
		// // 不够数据，返回
		// if (remaining < remainingCapacity + 2) {
		// int length = remaining > remainingCapacity ? remainingCapacity
		// : remaining;
		// value.fillData(buffer, length);
		// return false;
		// } else if (remainingCapacity > 0) {
		// value.fillData(buffer, value.getCapacity()
		// - value.getSize());
		// }
		//
		// // byte[] data = new byte[value.getCapacity()];
		// // buffer.get(data);
		// // value.setData(data);
		// buffer.position(buffer.position()
		// + ByteUtils.SPLIT.remaining());
		// this.currentReturnKey = null;
		// this.currentLine = null;
		// } else {
		// String line = currentLine;
		// this.currentLine = null;
		// return decodeError(line);
		//
		// }
		// } else
		// return false;
		// }
	}

	private String getItem(ByteBuffer buffer, char token, char... others) {
		final int limit = buffer.limit();
		if (this.currentPos == -1)
			this.currentPos = buffer.position();
		int pos = this.currentPos;
		for (; pos < limit; pos++) {
			byte b = buffer.get(pos);
			if (b == token || isIn(b, others)) {
				byte[] keyBytes = new byte[pos - buffer.position()];
				buffer.get(keyBytes);
				// skip token
				buffer.position(pos + 1);
				this.currentPos = -1;
				return getString(keyBytes);
			}
		}
		// Doesn't find token,remember current position
		this.currentPos = pos;
		return null;
	}

	private boolean isIn(byte b, char[] others) {
		for (int i = 0; i < others.length; i++) {
			if (b == others[i])
				return true;
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
		return returnValues;
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
				+ Constants.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(this.ioBuffer, cmdBytes, keyBytes);
		this.ioBuffer.flip();
	}

}
