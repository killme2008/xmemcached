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
package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;
/**
 * Store command for text protocol
 * @author dennis
 *
 */
public class TextStoreCommand extends Command {
	protected int expTime;
	protected long cas;
	protected Object value;

	@SuppressWarnings("unchecked")
	public TextStoreCommand(String key, byte[] keyBytes, CommandType cmdType,
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
		if (buffer == null || !buffer.hasRemaining()) {
			return false;
		}
		if (this.result == null) {
			byte first = buffer.get(buffer.position());
			if (first == 'S') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// STORED\r\n
				return ByteUtils.stepBuffer(buffer, 8);
			} else if (first == 'N') {
				setResult(Boolean.FALSE);
				countDownLatch();
				// NOT_STORED\r\n
				return ByteUtils.stepBuffer(buffer, 12);
			} else {
				return decodeError(session, buffer);
			}
		} else {
			Boolean result = (Boolean) this.result;
			if (result) {
				return ByteUtils.stepBuffer(buffer, 8);
			} else {
				return ByteUtils.stepBuffer(buffer, 12);
			}
		}
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		final CachedData data = encodeValue();
		byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlag()));
		byte[] expBytes = ByteUtils.getBytes(String.valueOf(this.expTime));
		byte[] dataLenBytes = ByteUtils.getBytes(String
				.valueOf(data.getData().length));
		byte[] casBytes = ByteUtils.getBytes(String.valueOf(this.cas));
		String cmdStr = this.commandType.name().toLowerCase();
		int size = cmdStr.length() + 1 + this.keyBytes.length + 1
				+ flagBytes.length + 1 + expBytes.length + 1
				+ data.getData().length + 2 * Constants.CRLF.length
				+ dataLenBytes.length;
		if (this.commandType == CommandType.CAS) {
			size += 1 + casBytes.length;
		}
		if (isNoreply()) {
			this.ioBuffer = bufferAllocator.allocate(size + 1
					+ Constants.NO_REPLY.length());
		} else {
			this.ioBuffer = bufferAllocator.allocate(size);
		}
		if (this.commandType == CommandType.CAS) {
			if (isNoreply()) {
				ByteUtils.setArguments(this.ioBuffer, cmdStr, this.keyBytes,
						flagBytes, expBytes, dataLenBytes, casBytes,
						Constants.NO_REPLY);
			} else {
				ByteUtils.setArguments(this.ioBuffer, cmdStr, this.keyBytes,
						flagBytes, expBytes, dataLenBytes, casBytes);
			}
		} else {
			if (isNoreply()) {
				ByteUtils.setArguments(this.ioBuffer, cmdStr, this.keyBytes,
						flagBytes, expBytes, dataLenBytes, Constants.NO_REPLY);
			} else {
				ByteUtils.setArguments(this.ioBuffer, cmdStr, this.keyBytes,
						flagBytes, expBytes, dataLenBytes);
			}
		}
		ByteUtils.setArguments(this.ioBuffer, data.getData());

		this.ioBuffer.flip();
	}

	@SuppressWarnings("unchecked")
	protected CachedData encodeValue() {
		final CachedData data = this.transcoder.encode(this.value);
		return data;
	}

}
