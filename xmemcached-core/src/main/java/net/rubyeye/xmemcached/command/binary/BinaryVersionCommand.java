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
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.transcoders.CachedData;
/**
 * Version command for binary protocol
 * @author boyan
 *
 */
public class BinaryVersionCommand extends BaseBinaryCommand implements
		ServerAddressAware {
	public InetSocketAddress server;

	public final InetSocketAddress getServer() {
		return this.server;
	}

	public final void setServer(InetSocketAddress server) {
		this.server = server;
	}

	public BinaryVersionCommand(final CountDownLatch latch,
			InetSocketAddress server) {
		super("[version]", null, CommandType.VERSION, latch, 0, 0, latch, false,
				null);
		this.commandType = CommandType.VERSION;
		this.server = server;
		this.opCode = OpCode.VERSION;
	}

	@Override
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		int valueLength = bodyLength - keyLength - extrasLength;
		if (buffer.remaining() < valueLength) {
			return false;
		}
		byte[] bytes = new byte[valueLength];
		buffer.get(bytes);
		setResult(new String(bytes));
		countDownLatch();
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
	protected void fillKey() {
		// must not have key
	}

	@Override
	protected int getKeyLength() {
		return 0;
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

}
