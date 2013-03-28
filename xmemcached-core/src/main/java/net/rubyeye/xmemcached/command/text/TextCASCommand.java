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

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * CAS command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextCASCommand extends TextStoreCommand {

	@SuppressWarnings("unchecked")
	public TextCASCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
	}

	private FailStatus failStatus;

	static enum FailStatus {
		NOT_FOUND, EXISTS
	}

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {

		if (buffer == null || !buffer.hasRemaining())
			return false;
		if (result == null) {
			if (buffer.remaining() < 2)
				return false;
			byte first = buffer.get(buffer.position());
			byte second = buffer.get(buffer.position() + 1);
			if (first == 'S' && second == 'T') {
				setResult(Boolean.TRUE);
				countDownLatch();
				// STORED\r\n
				return ByteUtils.stepBuffer(buffer, 8);
			} else if (first == 'N') {
				setResult(Boolean.FALSE);
				countDownLatch();
				failStatus = FailStatus.NOT_FOUND;
				// NOT_FOUND\r\n
				return ByteUtils.stepBuffer(buffer, 11);
			} else if (first == 'E' && second == 'X') {
				setResult(Boolean.FALSE);
				countDownLatch();
				failStatus = FailStatus.EXISTS;
				// EXISTS\r\n
				return ByteUtils.stepBuffer(buffer, 8);
			} else
				return decodeError(session, buffer);
		} else {
			Boolean result = (Boolean) this.result;
			if (result) {
				return ByteUtils.stepBuffer(buffer, 8);
			} else {
				switch (this.failStatus) {
				case NOT_FOUND:
					return ByteUtils.stepBuffer(buffer, 11);
				case EXISTS:
					return ByteUtils.stepBuffer(buffer, 8);
				default:
					return decodeError(session, buffer);
				}
			}
		}
	}
}
