package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedTextDecoder;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

public abstract class TextGetCommand extends Command {
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't', 's' };
	protected Map<String, CachedData> returnValues;
	private String currentReturnKey;

	public TextGetCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch) {
		super(key, keyBytes, cmdType, latch);
		this.returnValues = new HashMap<String, CachedData>();
	}

	protected boolean wasFirst = true;
	protected String currentLine;

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		while (true) {
			if (currentLine == null)
				currentLine = MemcachedTextDecoder.nextLine(session, buffer);
			if (currentLine != null) {
				if (currentLine.equals("END")) {
					dispatch();
					this.currentReturnKey = null;
					this.currentLine = null;
					return true;
				} else if (currentLine.startsWith("VALUE")) {
					wasFirst = false;
					if (currentReturnKey == null) {
						StringTokenizer stringTokenizer = new StringTokenizer(
								currentLine, " ");
						stringTokenizer.nextToken();
						this.currentReturnKey = stringTokenizer.nextToken();

						int flag = Integer
								.parseInt(stringTokenizer.nextToken());
						int dataLen = Integer.parseInt(stringTokenizer
								.nextToken());
						// maybe gets,it have cas value
						CachedData value = new CachedData(flag, null, dataLen,
								-1);
						if (stringTokenizer.hasMoreTokens()) {
							value.setCas(Long.parseLong(stringTokenizer
									.nextToken()));
						}
						this.returnValues.put(this.currentReturnKey, value);
					}

					CachedData value = this.returnValues
							.get(this.currentReturnKey);
					// 不够数据，返回
					if (buffer.remaining() < value.getDataLen() + 2) {
						return false;
					}

					byte[] data = new byte[value.getDataLen()];
					buffer.get(data);
					value.setData(data);
					buffer.position(buffer.position()
							+ MemcachedTextDecoder.SPLIT.remaining());
					this.currentReturnKey = null;
					this.currentLine = null;
				} else {
					decodeError(currentLine, null);
				}
			} else
				return false;
		}
	}

	public abstract void dispatch();

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		byte[] cmdBytes = (this.commandType == CommandType.GET_ONE
				|| this.commandType == CommandType.GET_MANY ? GET : GETS);
		this.ioBuffer = bufferAllocator.allocate(cmdBytes.length
				+ Constants.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(this.ioBuffer, cmdBytes, keyBytes);
		this.ioBuffer.flip();
	}

}
