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

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.StoreCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Store command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextStoreCommand extends Command implements StoreCommand {
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
			if (buffer.remaining() < 2)
				return false;
			int pos = buffer.position();
			byte first = buffer.get(pos);
			byte second = buffer.get(pos + 1);
			if (first == 'S' && second == 'T') {
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

	private String getCommandName() {
		switch (this.commandType) {
			case ADD :
				return "add";
			case SET :
				return "set";
			case REPLACE :
				return "replace";
			case APPEND :
				return "append";
			case PREPEND :
				return "prepend";
			case CAS :
				return "cas";
			default :
				throw new IllegalArgumentException(
						this.commandType.name() + " is not a store command");

		}
	}

	@Override
	public final void encode() {
		final CachedData data = encodeValue();
		String cmdStr = getCommandName();
		byte[] encodedData = data.getData();
		int flag = data.getFlag();
		int size = cmdStr.length() + this.keyBytes.length
				+ ByteUtils.stringSize(flag)
				+ ByteUtils.stringSize(this.expTime) + encodedData.length
				+ ByteUtils.stringSize(encodedData.length) + 8;
		if (this.commandType == CommandType.CAS) {
			size += 1 + ByteUtils.stringSize(this.cas);
		}
		byte[] buf;
		if (isNoreply()) {
			buf = new byte[size + 8];
		} else {
			buf = new byte[size];
		}
		int offset = 0;
		if (this.commandType == CommandType.CAS) {
			if (isNoreply()) {
				offset = ByteUtils.setArguments(buf, offset, cmdStr,
						this.keyBytes, flag, this.expTime, encodedData.length,
						this.cas, Constants.NO_REPLY);
			} else {
				offset = ByteUtils.setArguments(buf, offset, cmdStr,
						this.keyBytes, flag, this.expTime, encodedData.length,
						this.cas);
			}
		} else {
			if (isNoreply()) {
				offset = ByteUtils.setArguments(buf, offset, cmdStr,
						this.keyBytes, flag, this.expTime, encodedData.length,
						Constants.NO_REPLY);
			} else {
				offset = ByteUtils.setArguments(buf, offset, cmdStr,
						this.keyBytes, flag, this.expTime, encodedData.length);
			}
		}
		ByteUtils.setArguments(buf, offset, encodedData);
		this.ioBuffer = IoBuffer.wrap(buf);
	}

	@SuppressWarnings("unchecked")
	protected CachedData encodeValue() {
		final CachedData data = this.transcoder.encode(this.value);
		return data;
	}

}
