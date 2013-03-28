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
package net.rubyeye.xmemcached.command.kestrel;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextStoreCommand;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
/**
 * kestrel set command
 * @author dennis
 *
 */
public class KestrelSetCommand extends TextStoreCommand {

	@SuppressWarnings("unchecked")
	public KestrelSetCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected CachedData encodeValue() {

		final CachedData value = this.transcoder.encode(this.value);
		// If disable save primitive type as string,prepend 4 bytes flag to
		// value
		if (!this.transcoder.isPrimitiveAsString()) {
			int flags = value.getFlag();
			byte[] flagBytes = KestrelGetCommand.transcoderUtils
					.encodeInt(flags);
			byte[] origData = value.getData();
			byte[] newData = new byte[origData.length + 4];
			System.arraycopy(flagBytes, 0, newData, 0, 4);
			System.arraycopy(origData, 0, newData, 4, origData.length);
			value.setCapacity(newData.length);
			value.setData(newData);
		}
		return value;
	}

}
