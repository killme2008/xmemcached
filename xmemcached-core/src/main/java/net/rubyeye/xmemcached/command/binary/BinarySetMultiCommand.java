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
public class BinarySetMultiCommand extends BaseBinaryCommand implements
		MergeCommandsAware {
	private boolean finished;
	private Integer responseOpaque;
	private Map<Object, Command> mergeCommands;

	public BinarySetMultiCommand(String key, CommandType cmdType,
			CountDownLatch latch) {
		super(key, null, cmdType, latch, 0, 0, null, false, null);
		this.result = new HashMap<Integer/* opaque */, Boolean>();
	}

	@Override
	protected boolean readOpCode(ByteBuffer buffer) {
		byte opCode = buffer.get();
		// last response is SET,then finish decoding
		if (opCode == OpCode.SET.fieldValue()) {
			this.finished = true;
		}
		return true;
	}

	/**
	 * optimistic,if response status is greater than zero,then skip buffer to
	 * next response,set result to be false.
	 */
	protected void readHeader(ByteBuffer buffer) {
		super.readHeader(buffer);
		if (this.responseStatus != ResponseStatus.NO_ERROR) {
			if (ByteUtils.stepBuffer(buffer, this.responseTotalBodyLength)) {
				this.decodeStatus = BinaryDecodeStatus.DONE;
			}
		}
	}

	public Map<Object, Command> getMergeCommands() {
		return mergeCommands;
	}

	public void setMergeCommands(Map<Object, Command> mergeCommands) {
		this.mergeCommands = mergeCommands;
	}

	@Override
	public void encode() {
		// do nothing
	}

	@Override
	protected boolean finish() {
		final Boolean rt = ((Map<Integer, Boolean>) this.result)
				.get(this.responseOpaque);
		Map<Object, Command> mergetCommands = getMergeCommands();
		if (mergetCommands != null) {
			final BinaryStoreCommand command = (BinaryStoreCommand) mergetCommands
					.remove(this.responseOpaque);
			if (command != null) {
				command.setResult(rt);
				command.countDownLatch();
				this.mergeCount--;
			}
		}
		if (this.finished) {
			if (getMergeCommands() != null) {
				Collection<Command> mergeCommands = getMergeCommands().values();
				getIoBuffer().free();
				for (Command nextCommand : mergeCommands) {
					BinaryStoreCommand command = (BinaryStoreCommand) nextCommand;
					// Default result is true,it's quiet.
					command.setResult(Boolean.TRUE);
					command.countDownLatch();
					this.mergeCount--;
				}
			}
			assert (mergeCount == 0);
			countDownLatch();
		} else {
			this.responseOpaque = null;
		}
		return this.finished;
	}

	protected boolean readOpaque(ByteBuffer buffer) {
		responseOpaque = buffer.getInt();

		Command cmd = this.getMergeCommands().get(responseOpaque);
		if (cmd == null) {
			// It's not this command's merged commands,we must ignore it.
			return false;
		} else {
			if (this.responseStatus == ResponseStatus.NO_ERROR) {
				((Map<Integer, Boolean>) this.result).put(responseOpaque,
						Boolean.TRUE);
			} else {
				((Map<Integer, Boolean>) this.result).put(responseOpaque,
						Boolean.FALSE);
			}
			return true;
		}

	}
}
