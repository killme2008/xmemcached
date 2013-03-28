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

import net.rubyeye.xmemcached.command.VerbosityCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Verbosity command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextVerbosityCommand extends VerbosityCommand {

	public static final String VERBOSITY = "verbosity";

	public TextVerbosityCommand(CountDownLatch latch, int level, boolean noreply) {
		super(latch, level, noreply);

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
	public void encode() {
		final byte[] levelBytes = ByteUtils
				.getBytes(String.valueOf(this.level));
		if (isNoreply()) {
			this.ioBuffer = IoBuffer.allocate(4 + VERBOSITY.length()
					+ levelBytes.length + Constants.NO_REPLY.length);
			ByteUtils.setArguments(this.ioBuffer, VERBOSITY, levelBytes,
					Constants.NO_REPLY);
		} else {
			this.ioBuffer = IoBuffer.allocate(2 + 1 + VERBOSITY.length()
					+ levelBytes.length);
			ByteUtils.setArguments(this.ioBuffer, VERBOSITY, levelBytes);

		}
		this.ioBuffer.flip();
	}
}
