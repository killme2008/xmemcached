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
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Touch command for touch protocol.
 * 
 * @author dennis<killme2008@gmail.com>
 * @since 1.3.8
 * 
 */
public class TextTouchCommand extends Command {

	private static final String NOT_FOUND = "NOT_FOUND\r\n";
	private static final String TOUCHED = "TOUCHED\r\n";
	private int expTime;

	public TextTouchCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int expTime, boolean noreply) {
		super(key, keyBytes, cmdType, latch);
		this.expTime = expTime;
		this.noreply = noreply;
	}

	public int getExpTime() {
		return expTime;
	}

	public void setExpTime(int expTime) {
		this.expTime = expTime;
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining()) {
			return false;
		}
		if (this.result == null) {
			if (buffer.remaining() < 1)
				return false;
			byte first = buffer.get(buffer.position());
			if (first == 'T') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// TOUCHED\r\n
				return ByteUtils.stepBuffer(buffer, TOUCHED.length());
			} else if (first == 'N') {
				setResult(Boolean.FALSE);
				countDownLatch();
				// NOT_FOUND\r\n
				return ByteUtils.stepBuffer(buffer, NOT_FOUND.length());
			} else {
				return decodeError(session, buffer);
			}
		} else {
			Boolean result = (Boolean) this.result;
			if (result) {
				return ByteUtils.stepBuffer(buffer, TOUCHED.length());
			} else {
				return ByteUtils.stepBuffer(buffer, NOT_FOUND.length());
			}
		}
	}

	@Override
	public final void encode() {
		byte[] cmdBytes = Constants.TOUCH;
		int size = 7 + this.keyBytes.length
				+ ByteUtils.stringSize(this.expTime) + Constants.CRLF.length;
		if (isNoreply()) {
			size += 8;
		}
		byte[] buf = new byte[size];
		if (isNoreply()) {
			ByteUtils.setArguments(buf, 0, cmdBytes, this.keyBytes,
					this.expTime, Constants.NO_REPLY);
		} else {
			ByteUtils.setArguments(buf, 0, cmdBytes, this.keyBytes,
					this.expTime);
		}
		this.ioBuffer = IoBuffer.wrap(buf);
	}

}
