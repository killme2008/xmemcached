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
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.AssocCommandAware;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Base binary protocol implementation
 * 
 * @author dennis
 * 
 */
public class BinaryStoreCommand extends BaseBinaryCommand {

	public BinaryStoreCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
		switch (cmdType) {
		case SET:
			this.opCode = noreply ? OpCode.SET_QUIETLY : OpCode.SET;
			break;
		case REPLACE:
			this.opCode = noreply ? OpCode.REPLACE_QUIETLY : OpCode.REPLACE;
			break;
		case ADD:
			this.opCode = noreply ? OpCode.ADD_QUIETLY : OpCode.ADD;
			break;
		case SET_MANY:
			//ignore
			break;
		default:
			throw new IllegalArgumentException(
					"Unknow cmd type for storage commands:" + cmdType);

		}
	}

	/**
	 * optimistic,if no error,goto done
	 */
	protected void readHeader(ByteBuffer buffer) {
		super.readHeader(buffer);
		if (this.responseStatus == ResponseStatus.NO_ERROR) {
			this.decodeStatus = BinaryDecodeStatus.DONE;
		}
	}

}
