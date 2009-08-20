package net.rubyeye.xmemcached.command.binary;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.exception.MemcachedDecodeException;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

public abstract class BaseBinaryCommand extends Command {
	protected int expTime;
	protected long cas;
	protected Object value;

	protected OpCode opCode;
	protected BinaryDecodeStatus decodeStatus = BinaryDecodeStatus.NONE;
	protected int responseKeyLength, responseExtrasLength,
			responseTotalBodyLength;
	protected ResponseStatus responseStatus;

	@SuppressWarnings("unchecked")
	public BaseBinaryCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch);
		this.expTime = exp;
		this.cas = cas;
		this.value = value;
		this.noreply = noreply;
		this.transcoder = transcoder;
	}

	public final int getExpTime() {
		return this.expTime;
	}

	public final void setExpTime(int exp) {
		this.expTime = exp;
	}

	public final long getCas() {
		return this.cas;
	}

	public final void setCas(long cas) {
		this.cas = cas;
	}

	public final Object getValue() {
		return this.value;
	}

	public final void setValue(Object value) {
		this.value = value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Transcoder getTranscoder() {
		return this.transcoder;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		while (true) {
			LABEL: switch (this.decodeStatus) {
			case NONE:
				if (buffer.remaining() < 24) {
					return false;
				} else {
					this.decodeStatus = BinaryDecodeStatus.READ_HEADER;
				}
				continue;
			case READ_HEADER:
				readHeader(buffer);
				this.decodeStatus = BinaryDecodeStatus.READ_EXTRAS;
				continue;
			case READ_EXTRAS:
				if (readExtras(buffer, this.responseExtrasLength)) {
					this.decodeStatus = BinaryDecodeStatus.READ_KEY;
					continue;
				} else {
					return false;
				}
			case READ_KEY:
				if (readKey(buffer, this.responseKeyLength)) {
					this.decodeStatus = BinaryDecodeStatus.READ_VALUE;
					continue;
				} else {
					return false;
				}
			case READ_VALUE:
				if (readValue(buffer, this.responseTotalBodyLength,
						this.responseKeyLength, this.responseExtrasLength)) {
					this.decodeStatus = BinaryDecodeStatus.DONE;
					continue;
				} else {
					return false;
				}
			case DONE:
				if (finish()) {
					return true;
				} else {
					// Do not finish,continue to decode
					this.decodeStatus = BinaryDecodeStatus.NONE;
					break LABEL;
				}
			}
		}
	}

	protected boolean finish() {
		if (this.result == null) {
			if (this.responseStatus == ResponseStatus.NO_ERROR) {
				setResult(Boolean.TRUE);
			} else {
				setResult(Boolean.FALSE);
			}
		}
		countDownLatch();
		return true;
	}

	protected void readHeader(ByteBuffer buffer) {
		readMagicNumber(buffer);
		readOpCode(buffer);
		readKeyLength(buffer);
		readExtrasLength(buffer);
		readDataType(buffer);
		readStatus(buffer);
		readBodyLength(buffer);
		readOpaque(buffer);
		readCAS(buffer);

	}

	protected void readOpaque(ByteBuffer buffer) {
		ByteUtils.stepBuffer(buffer, 4);
	}

	protected long readCAS(ByteBuffer buffer) {
		ByteUtils.stepBuffer(buffer, 8);
		return 0;
	}

	protected boolean readKey(ByteBuffer buffer, int keyLength) {
		return ByteUtils.stepBuffer(buffer, keyLength);
	}

	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		if (!ByteUtils
				.stepBuffer(buffer, bodyLength - keyLength - extrasLength)) {
			throw new MemcachedDecodeException(
					"Binary command decode error,buffer remaining less than value length:"
							+ (bodyLength - keyLength - extrasLength));
		}
		return true;
	}

	protected boolean readExtras(ByteBuffer buffer, int extrasLength) {
		return ByteUtils.stepBuffer(buffer, extrasLength);
	}

	private int readBodyLength(ByteBuffer buffer) {
		this.responseTotalBodyLength = buffer.getInt();
		return this.responseTotalBodyLength;
	}

	protected void readStatus(ByteBuffer buffer) {
		this.responseStatus = ResponseStatus.parseShort(buffer.getShort());
		if (this.responseStatus == ResponseStatus.UNKNOWN_COMMAND) {
			setException(new UnknownCommandException());
		}
	}

	public final OpCode getOpCode() {
		return this.opCode;
	}

	public final void setOpCode(OpCode opCode) {
		this.opCode = opCode;
	}

	public final ResponseStatus getResponseStatus() {
		return this.responseStatus;
	}

	public final void setResponseStatus(ResponseStatus responseStatus) {
		this.responseStatus = responseStatus;
	}

	private int readKeyLength(ByteBuffer buffer) {
		this.responseKeyLength = buffer.getShort();
		return this.responseKeyLength;
	}

	private int readExtrasLength(ByteBuffer buffer) {
		this.responseExtrasLength = buffer.get();
		return this.responseExtrasLength;
	}

	private byte readDataType(ByteBuffer buffer) {
		return buffer.get();
	}

	protected void readOpCode(ByteBuffer buffer) {
		byte op = buffer.get();

		if (op != this.opCode.fieldValue()) {
			throw new MemcachedDecodeException("Not a proper "
					+ this.opCode.name() + " response");
		}
	}

	private void readMagicNumber(ByteBuffer buffer) {
		byte magic = buffer.get();

		if (magic != RESPONSE_MAGIC_NUMBER) {
			throw new MemcachedDecodeException("Not a proper response");
		}
	}

	/**
	 * Set,add,replace protocol's extras length
	 */
	static final byte EXTRAS_LENGTH = (byte) 8;
	

	@Override
	@SuppressWarnings("unchecked")
	public void encode(BufferAllocator bufferAllocator) {
		CachedData data = null;
		if (this.transcoder != null) {
			data = this.transcoder.encode(this.value);
		}
		// header+key+value+extras
		int length = 24 + getKeyLength() + getValueLength(data)
				+ getExtrasLength();

		this.ioBuffer = bufferAllocator.allocate(length);
		fillHeader(data);
		fillExtras(data);
		fillKey();
		fillValue(data);

		this.ioBuffer.flip();

	}

	protected void fillValue(final CachedData data) {
		this.ioBuffer.put(data.getData());
	}

	protected void fillKey() {
		this.ioBuffer.put(this.keyBytes);
	}

	protected void fillExtras(final CachedData data) {
		this.ioBuffer.putInt(data.getFlag());
		this.ioBuffer.putInt(this.expTime);
	}

	protected final void fillHeader(final CachedData data) {
		this.ioBuffer.put(REQUEST_MAGIC_NUMBER);
		this.ioBuffer.put(this.opCode.fieldValue());
		this.ioBuffer.putShort((short) getKeyLength());
		this.ioBuffer.put(getExtrasLength());
		// Data type
		this.ioBuffer.put((byte) 0);
		// Reserved
		this.ioBuffer.putShort((short) 0);

		this.ioBuffer.putInt(getExtrasLength() + getKeyLength()
				+ getValueLength(data));
		// Opaque
		this.ioBuffer.putInt(0);
		// CAS
		this.ioBuffer.putLong(0L);
	}

	protected int getValueLength(final CachedData data) {
		return data.getData().length;
	}

	protected int getKeyLength() {
		return this.keyBytes.length;
	}

	protected byte getExtrasLength() {
		return EXTRAS_LENGTH;
	}

}
