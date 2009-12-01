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
 * Incr/Decr command for text protocol
 * @author dennis
 *
 */
public class TextIncrDecrCommand extends Command {

	private long amount;
	private final long initial;

	public TextIncrDecrCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, long increment,
			long initial, boolean noreply) {
		super(key, keyBytes, cmdType, latch);
		this.amount = increment;
		this.noreply = noreply;
		this.initial = initial;
	}

	public final long getAmount() {
		return this.amount;
	}

	public final void setAmount(int increment) {
		this.amount = increment;
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
				setResult(Long.parseLong(line));
				countDownLatch();
				return true;
			}
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		byte[] numBytes = ByteUtils.getBytes(String.valueOf(this.getAmount()));
		byte[] cmdBytes = this.commandType == CommandType.INCR ? Constants.INCR : Constants.DECR;
		int capacity = cmdBytes.length + 2 + this.key.length()
				+ numBytes.length + Constants.CRLF.length;
		if (isNoreply()) {
			capacity += 1 + Constants.NO_REPLY.length();
		}
		this.ioBuffer = bufferAllocator.allocate(capacity);
		if (isNoreply()) {
			ByteUtils.setArguments(this.ioBuffer, cmdBytes, this.keyBytes,
					numBytes, Constants.NO_REPLY);
		} else {
			ByteUtils.setArguments(this.ioBuffer, cmdBytes, this.keyBytes,
					numBytes);
		}
		this.ioBuffer.flip();

	}

}
