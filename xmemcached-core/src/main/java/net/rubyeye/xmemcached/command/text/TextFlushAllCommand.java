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
 * FlushAll command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextFlushAllCommand extends Command {

	public static final ByteBuffer FLUSH_ALL = ByteBuffer.wrap("flush_all\r\n"
			.getBytes());

	protected int exptime;

	public final int getExptime() {
		return this.exptime;
	}

	public TextFlushAllCommand(final CountDownLatch latch, int delay,
			boolean noreply) {
		super("[flush_all]", (byte[]) null, latch);
		this.commandType = CommandType.FLUSH_ALL;
		this.exptime = delay;
		this.noreply = noreply;
	}

	@Override
	public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining()) {
			return false;
		}
		if (this.result == null) {
			byte first = buffer.get(buffer.position());
			if (first == 'O') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// OK\r\n
				return ByteUtils.stepBuffer(buffer, 4);
			} else {
				return decodeError(session, buffer);
			}
		} else {
			return ByteUtils.stepBuffer(buffer, 4);
		}
	}

	@Override
	public final void encode() {
		if (isNoreply()) {
			if (this.exptime <= 0) {
				this.ioBuffer = IoBuffer.allocate("flush_all".length() + 1
						+ Constants.NO_REPLY.length + 2);
				ByteUtils.setArguments(this.ioBuffer, "flush_all",
						Constants.NO_REPLY);
			} else {
				byte[] delayBytes = ByteUtils.getBytes(String
						.valueOf(this.exptime));
				this.ioBuffer = IoBuffer.allocate("flush_all".length() + 2
						+ delayBytes.length + Constants.NO_REPLY.length + 2);
				ByteUtils.setArguments(this.ioBuffer, "flush_all", delayBytes,
						Constants.NO_REPLY);
			}
			this.ioBuffer.flip();
		} else {
			if (this.exptime <= 0) {
				this.ioBuffer = IoBuffer.wrap(FLUSH_ALL.slice());
			} else {

				byte[] delayBytes = ByteUtils.getBytes(String
						.valueOf(this.exptime));
				this.ioBuffer = IoBuffer.allocate("flush_all".length() + 1
						+ delayBytes.length + 2);
				ByteUtils.setArguments(this.ioBuffer, "flush_all", delayBytes);
			}
		}
	}

}
