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
 * Incr/Decr command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextIncrDecrCommand extends Command {

	private long delta;
	private final long initial;

	public TextIncrDecrCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, long delta,
			long initial, boolean noreply) {
		super(key, keyBytes, cmdType, latch);
		this.delta = delta;
		this.noreply = noreply;
		this.initial = initial;
	}

	public final long getDelta() {
		return this.delta;
	}

	public final void setDelta(int delta) {
		this.delta = delta;
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = ByteUtils.nextLine(buffer);
		if (line != null) {
			if (line.equals("NOT_FOUND")) {
				// setException(new MemcachedException(
				// "The key's value is not found for increase or decrease"));
				setResult("NOT_FOUND");
				countDownLatch();
				return true;
			} else {
				String rt = line.trim();
				if (Character.isDigit(rt.charAt(0))) {
					setResult(Long.valueOf(rt));
					countDownLatch();
					return true;
				} else {
					return decodeError(line);
				}
			}
		}
		return false;
	}

	@Override
	public final void encode() {
		byte[] cmdBytes = this.commandType == CommandType.INCR ? Constants.INCR
				: Constants.DECR;
		int size = 6 + this.keyBytes.length
				+ ByteUtils.stringSize(this.getDelta()) + Constants.CRLF.length;
		if (isNoreply()) {
			size += 8;
		}
		byte[] buf = new byte[size];
		if (isNoreply()) {
			ByteUtils.setArguments(buf, 0, cmdBytes, this.keyBytes,
					this.getDelta(), Constants.NO_REPLY);
		} else {
			ByteUtils.setArguments(buf, 0, cmdBytes, this.keyBytes,
					this.getDelta());
		}
		this.ioBuffer = IoBuffer.wrap(buf);
	}

}
