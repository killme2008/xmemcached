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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.MapReturnValueAware;
import net.rubyeye.xmemcached.command.MergeCommandsAware;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * A command for holding getkq commands
 * 
 * @author dennis
 * 
 */
@SuppressWarnings("unchecked")
public class BinaryGetMultiCommand extends BaseBinaryCommand implements
		MergeCommandsAware, MapReturnValueAware {
	private boolean finished;
	private String responseKey;
	private long responseCAS;
	private int responseFlag;
	private Map<Object, Command> mergeCommands;

	public BinaryGetMultiCommand(String key, CommandType cmdType,
			CountDownLatch latch) {
		super(key, null, cmdType, latch, 0, 0, null, false, null);
		this.result = new HashMap<String, CachedData>();
	}

	public Map<String, CachedData> getReturnValues() {
		return (Map<String, CachedData>) this.result;
	}

	@Override
	protected boolean readOpCode(ByteBuffer buffer) {
		byte opCode = buffer.get();
		// last response is GET_KEY,then finish decoding
		if (opCode == OpCode.GET_KEY.fieldValue()) {
			this.finished = true;
		}
		return true;
	}

	/**
	 * optimistic,if response status is greater than zero,then skip buffer to
	 * next response,set result as null
	 */
	protected void readHeader(ByteBuffer buffer) {
		super.readHeader(buffer);
		if (this.responseStatus != ResponseStatus.NO_ERROR) {
			if (ByteUtils.stepBuffer(buffer, this.responseTotalBodyLength)) {
				this.decodeStatus = BinaryDecodeStatus.DONE;
			}
		}
	}

	@Override
	public void encode() {
		// do nothing
	}

	@Override
	protected boolean finish() {
		final CachedData cachedData = ((Map<String, CachedData>) this.result)
				.get(this.responseKey);
		Map<Object, Command> mergetCommands = getMergeCommands();
		if (mergetCommands != null) {
			final BinaryGetCommand command = (BinaryGetCommand) mergetCommands
					.remove(this.responseKey);
			if (command != null) {
				command.setResult(cachedData);
				command.countDownLatch();
				this.mergeCount--;
				if (command.getAssocCommands() != null) {
					for (Command assocCommand : command.getAssocCommands()) {
						assocCommand.setResult(cachedData);
						assocCommand.countDownLatch();
						this.mergeCount--;
					}
				}

			}
		}
		if (this.finished) {
			if (getMergeCommands() != null) {
				Collection<Command> mergeCommands = getMergeCommands().values();
				getIoBuffer().free();
				for (Command nextCommand : mergeCommands) {
					BinaryGetCommand command = (BinaryGetCommand) nextCommand;
					command.countDownLatch();
					if (command.getAssocCommands() != null) {
						for (Command assocCommand : command.getAssocCommands()) {
							assocCommand.countDownLatch();
						}
					}
				}
			}
			countDownLatch();
		} else {

			this.responseKey = null;
		}
		return this.finished;
	}

	@Override
	protected boolean readKey(ByteBuffer buffer, int keyLength) {
		if (buffer.remaining() < keyLength) {
			return false;
		}
		if (keyLength > 0) {
			byte[] bytes = new byte[keyLength];
			buffer.get(bytes);
			this.responseKey = ByteUtils.getString(bytes);
			CachedData value = new CachedData();
			value.setCas(this.responseCAS);
			value.setFlag(this.responseFlag);
			((Map<String, CachedData>) this.result)
					.put(this.responseKey, value);
		}
		return true;
	}

	@Override
	protected boolean readValue(ByteBuffer buffer, int bodyLength,
			int keyLength, int extrasLength) {
		if (this.responseStatus == ResponseStatus.NO_ERROR) {
			int valueLength = bodyLength - keyLength - extrasLength;
			CachedData responseValue = ((Map<String, CachedData>) this.result)
					.get(this.responseKey);
			if (valueLength >= 0 && responseValue.getCapacity() < 0) {
				responseValue.setCapacity(valueLength);
				responseValue.setData(new byte[valueLength]);
			}
			int remainingCapacity = responseValue.remainingCapacity();
			int remaining = buffer.remaining();
			if (remaining < remainingCapacity) {
				int length = remaining > remainingCapacity ? remainingCapacity
						: remaining;
				responseValue.fillData(buffer, length);
				return false;
			} else if (remainingCapacity > 0) {
				responseValue.fillData(buffer, remainingCapacity);
			}
			return true;
		} else {
			((Map<String, CachedData>) this.result).remove(this.responseKey);
			return true;
		}
	}

	@Override
	protected boolean readExtras(ByteBuffer buffer, int extrasLength) {
		if (buffer.remaining() < extrasLength) {
			return false;
		}
		if (extrasLength == 4) {
			// read flag
			this.responseFlag = buffer.getInt();
		}
		return true;
	}

	@Override
	protected long readCAS(ByteBuffer buffer) {
		this.responseCAS = buffer.getLong();
		return this.responseCAS;
	}

	public Map<Object, Command> getMergeCommands() {
		return this.mergeCommands;
	}

	public void setMergeCommands(Map<Object, Command> mergeCommands) {
		this.mergeCommands = mergeCommands;
	}

}
