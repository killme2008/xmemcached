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
 * Binary incr/decr command
 * @author dennis
 *
 */
public class BinaryIncrDecrCommand extends BaseBinaryCommand {

	private long amount, initial;

	public final long getAmount() {
		return this.amount;
	}

	public final void setAmount(long amount) {
		this.amount = amount;
	}

	public final long getInitial() {
		return this.initial;
	}

	public final void setInitial(long initial) {
		this.initial = initial;
	}

	public BinaryIncrDecrCommand(String key, byte[] keyBytes, long amount,
			long initial, int expTime, CommandType cmdType, boolean noreply) {
		super(key, keyBytes, cmdType, new CountDownLatch(1), 0, 0, null,
				noreply, null);
		this.amount = amount;
		this.initial = initial;
		this.expTime = expTime;
		switch (cmdType) {
		case INCR:
			this.opCode = noreply ? OpCode.INCREMENT_QUIETLY : OpCode.INCREMENT;
			break;
		case DECR:
			this.opCode = noreply ? OpCode.DECREMENT_QUIETLY : OpCode.DECREMENT;
			break;
		default:
			throw new IllegalArgumentException("Unknow cmd type for incr/decr:"
					+ cmdType);
		}

	}

	@Override
	protected void fillExtras(CachedData data) {
		this.ioBuffer.putLong(this.amount);
		this.ioBuffer.putLong(this.initial);
		this.ioBuffer.putInt(this.expTime);
	}

	@Override
	protected byte getExtrasLength() {
		return 20;
	}

	@Override
	protected void fillValue(CachedData data) {
		// must not have value
	}

	@Override
	protected int getValueLength(CachedData data) {
		return 0;
	}

	@Override
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		if (buffer.remaining() < 8) {
			return false;
		}
		long returnValue = buffer.getLong();
		setResult(returnValue);
		return true;
	}

}
