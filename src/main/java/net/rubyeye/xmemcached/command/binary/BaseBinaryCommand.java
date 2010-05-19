/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.command.binary;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.exception.MemcachedDecodeException;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.OpaqueGenerater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Base Binary command.
 * 
 * @author dennis
 * 
 */
public abstract class BaseBinaryCommand extends Command {
	private static final Logger log = LoggerFactory
			.getLogger(BinaryStoreCommand.class);
	protected int expTime;
	protected long cas;
	protected Object value;

	protected OpCode opCode;
	protected BinaryDecodeStatus decodeStatus = BinaryDecodeStatus.NONE;
	protected int responseKeyLength, responseExtrasLength,
			responseTotalBodyLength;
	protected ResponseStatus responseStatus;
	protected int opaque;

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
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
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
			case IGNORE:
				buffer.reset();
				return true;
			}
		}
	}

	protected boolean finish() {
		if (this.result == null) {
			if (this.responseStatus == ResponseStatus.NO_ERROR) {
				setResult(Boolean.TRUE);
			} else {
				switch (this.responseStatus) {
				case VALUE_TOO_BIG:
					log.error(ResponseStatus.VALUE_TOO_BIG.errorMessage());
					break;
				case INVALID_ARGUMENTS:
					log.error(ResponseStatus.INVALID_ARGUMENTS.errorMessage());
					break;
				default:
					log.debug(this.responseStatus.errorMessage());
				}
				setResult(Boolean.FALSE);
			}
		}
		countDownLatch();
		return true;
	}

	protected void readHeader(ByteBuffer buffer) {
		markBuffer(buffer);
		readMagicNumber(buffer);
		if (!readOpCode(buffer)) {
			this.decodeStatus = BinaryDecodeStatus.IGNORE;
			return;
		}
		readKeyLength(buffer);
		readExtrasLength(buffer);
		readDataType(buffer);
		readStatus(buffer);
		readBodyLength(buffer);
		if (!readOpaque(buffer)) {
			this.decodeStatus = BinaryDecodeStatus.IGNORE;
			return;
		}
		this.decodeStatus = BinaryDecodeStatus.READ_EXTRAS;
		readCAS(buffer);

	}

	private void markBuffer(ByteBuffer buffer) {
		buffer.mark();
	}

	protected boolean readOpaque(ByteBuffer buffer) {
		if (this.noreply) {
			int returnOpaque = buffer.getInt();
			if (returnOpaque != this.opaque) {
				return false;
			}
		} else {
			ByteUtils.stepBuffer(buffer, 4);
		}
		return true;
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
		return ByteUtils.stepBuffer(buffer, bodyLength - keyLength
				- extrasLength);
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
		if (this.responseStatus == ResponseStatus.AUTH_REQUIRED) {
			setException(new MemcachedServerException(this.responseStatus
					.errorMessage()));
		}
		if (this.responseStatus == ResponseStatus.FUTHER_AUTH_REQUIRED) {
			setException(new MemcachedServerException(this.responseStatus
					.errorMessage()));
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

	protected boolean readOpCode(ByteBuffer buffer) {
		byte op = buffer.get();
		if (op != this.opCode.fieldValue()) {
			if (this.noreply) {
				return false;
			} else {
				throw new MemcachedDecodeException("Not a proper "
						+ this.opCode.name() + " response");
			}
		}
		return true;
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
	public void encode() {
		CachedData data = null;
		if (this.transcoder != null) {
			data = this.transcoder.encode(this.value);
		}
		// header+key+value+extras
		int length = 24 + getKeyLength() + getValueLength(data)
				+ getExtrasLength();

		this.ioBuffer = IoBuffer.allocate(length);
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
		fillMagicNumber();
		fillOpCode();
		fillKeyLength();
		fillExtrasLength();
		fillDataType();
		fillReserved();
		fillTotalBodyLength(data);
		fillOpaque();
		fillCAS();
	}

	protected void fillCAS() {
		// CAS
		this.ioBuffer.putLong(0L);
	}

	private void fillOpaque() {
		// Opaque
		if (this.noreply) {
			this.opaque = OpaqueGenerater.getInstance().getNextValue();
		}
		this.ioBuffer.putInt(this.opaque);
	}

	private void fillTotalBodyLength(final CachedData data) {
		this.ioBuffer.putInt(getExtrasLength() + getKeyLength()
				+ getValueLength(data));
	}

	private void fillReserved() {
		// Reserved
		this.ioBuffer.putShort((short) 0);
	}

	private void fillDataType() {
		// Data type
		this.ioBuffer.put((byte) 0);
	}

	private void fillExtrasLength() {
		this.ioBuffer.put(getExtrasLength());
	}

	private void fillKeyLength() {
		this.ioBuffer.putShort((short) getKeyLength());
	}

	private void fillOpCode() {
		this.ioBuffer.put(this.opCode.fieldValue());
	}

	private void fillMagicNumber() {
		this.ioBuffer.put(REQUEST_MAGIC_NUMBER);
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
