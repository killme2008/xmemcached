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

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;
/**
 * Binary delete command
 * @author boyan
 *
 */
public class BinaryDeleteCommand extends BaseBinaryCommand {

	public BinaryDeleteCommand(String key, byte[] keyBytes, long cas,
			CommandType cmdType, CountDownLatch latch, boolean noreply) {
		super(key, keyBytes, cmdType, latch, 0, cas, null, noreply, null);
		this.opCode = noreply?OpCode.DELETE_QUIETLY:OpCode.DELETE;
	}

	/**
	 * optimistic,if no error,goto done
	 */
	@Override
	protected void readHeader(ByteBuffer buffer) {
		super.readHeader(buffer);
		if (this.responseStatus == ResponseStatus.NO_ERROR) {
			this.decodeStatus = BinaryDecodeStatus.DONE;
		}

	}

	@Override
	protected void fillExtras(CachedData data) {
		// must not have extras
	}

	@Override
	protected byte getExtrasLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
	}

}
