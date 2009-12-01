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
import net.rubyeye.xmemcached.utils.ByteUtils;
/**
 * Delete command for text protocol
 * @author dennis
 *
 */
public class TextDeleteCommand extends Command {

	protected int time;

	public TextDeleteCommand(String key, byte[] keyBytes, int time,
			final CountDownLatch latch, boolean noreply) {
		super(key, keyBytes, latch);
		this.commandType = CommandType.DELETE;
		this.time = time;
		this.noreply = noreply;
	}

	public final int getTime() {
		return this.time;
	}

	public final void setTime(int time) {
		this.time = time;
	}

	

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining()) {
			return false;
		}
		if (this.result == null) {
			byte first = buffer.get(buffer.position());
			if (first == 'D') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// DELETED\r\n
				return ByteUtils.stepBuffer(buffer, 9);
			} else if (first == 'N') {
				setResult(Boolean.FALSE);
				countDownLatch();
				// NOT_FOUND\r\n
				return ByteUtils.stepBuffer(buffer, 11);
			} else {
				return decodeError(session, buffer);
			}
		} else {
			Boolean result = (Boolean) this.result;
			if (result) {
				return ByteUtils.stepBuffer(buffer, 9);
			} else {
				return ByteUtils.stepBuffer(buffer, 11);
			}
		}
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		byte[] timeBytes = ByteUtils.getBytes(String.valueOf(this.time));
		int capacity = Constants.DELETE.length + 1
				+ this.keyBytes.length + Constants.CRLF.length;
		if (this.time > 0) {
			capacity += 1 + timeBytes.length;
		}
		if (isNoreply()) {
			capacity += 1 + Constants.NO_REPLY.length();
		}
		this.ioBuffer = bufferAllocator.allocate(capacity);
		if (isNoreply()) {
			if (this.time > 0) {
				ByteUtils.setArguments(this.ioBuffer, Constants.DELETE,
						this.keyBytes, timeBytes, Constants.NO_REPLY);
			} else {
				ByteUtils.setArguments(this.ioBuffer, Constants.DELETE,
						this.keyBytes, Constants.NO_REPLY);
			}
		} else {
			if (this.time > 0) {
				ByteUtils.setArguments(this.ioBuffer, Constants.DELETE,
						this.keyBytes, timeBytes);
			} else {
				ByteUtils.setArguments(this.ioBuffer, Constants.DELETE,
						this.keyBytes);
			}
		}
		this.ioBuffer.flip();
	}

}
