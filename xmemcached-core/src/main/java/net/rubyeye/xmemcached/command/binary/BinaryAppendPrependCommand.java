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

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Binary protocol for append,prepend
 * 
 * @author dennis
 * 
 */
@SuppressWarnings("unchecked")
public class BinaryAppendPrependCommand extends BaseBinaryCommand {

	public BinaryAppendPrependCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, int exp, long cas,
			Object value, boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
		switch (cmdType) {
		case APPEND:
			this.opCode = noreply ? OpCode.APPEND_QUIETLY : OpCode.APPEND;
			break;
		case PREPEND:
			this.opCode = noreply ? OpCode.PREPEND_QUIETLY : OpCode.PREPEND;
			break;
		default:
			throw new UnknownCommandException(
					"Not a append or prepend command:" + cmdType.name());
		}
	}

	@Override
	protected void fillExtras(CachedData data) {
		// no extras
	}

	@Override
	protected byte getExtrasLength() {
		return 0;
	}

}
