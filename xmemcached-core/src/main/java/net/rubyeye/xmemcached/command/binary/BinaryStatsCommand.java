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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;
/**
 * Stats command for binary protocol
 * @author boyan
 *
 */
public class BinaryStatsCommand extends BaseBinaryCommand implements
		ServerAddressAware {

	private InetSocketAddress server;
	private String itemName;
	private String currentResponseItem;

	public String getItemName() {
		return this.itemName;
	}

	public final InetSocketAddress getServer() {
		return this.server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}

	public void setItemName(String item) {
		this.itemName = item;
	}

	public BinaryStatsCommand(InetSocketAddress server, CountDownLatch latch,
			String itemName) {
		super(null, null, CommandType.STATS, latch, 0, 0, null, false, null);
		this.server = server;
		this.itemName = itemName;
		this.opCode=OpCode.STAT;
		this.result = new HashMap<String, String>();
	}

	@Override
	protected boolean finish() {
		// last packet
		if (this.currentResponseItem == null) {
			return super.finish();
		} else {
			// continue decode
			this.currentResponseItem = null;
			return false;
		}
	}

	
	
	@Override
	protected void readStatus(ByteBuffer buffer) {
		ResponseStatus responseStatus = ResponseStatus.parseShort(buffer
				.getShort());
		if (responseStatus == ResponseStatus.UNKNOWN_COMMAND) {
			setException(new UnknownCommandException());
		}
	}

	@Override
	protected boolean readKey(ByteBuffer buffer, int keyLength) {
		if (buffer.remaining() < keyLength) {
			return false;
		}
		if (keyLength > 0) {
			byte[] bytes = new byte[keyLength];
			buffer.get(bytes);
			this.currentResponseItem = new String(bytes);
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		int valueLength = bodyLength - keyLength - extrasLength;
		if (buffer.remaining() < valueLength) {
			return false;
		}
		if (valueLength > 0) {
			byte[] bytes = new byte[valueLength];
			buffer.get(bytes);
			String value = new String(bytes);
			((Map<String, String>) this.result).put(this.currentResponseItem,
					value);
		}
		return true;
	}

	@Override
	protected void fillExtras(CachedData data) {
		// must not have extras
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
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
	protected void fillKey() {
		if (this.itemName != null) {
			byte[] keyBytes = ByteUtils.getBytes(this.itemName);
			this.ioBuffer.put(keyBytes);
		}
	}

	@Override
	protected int getKeyLength() {
		if (this.itemName != null) {
			return this.itemName.length();
		} else {
			return 0;
		}
	}

}
