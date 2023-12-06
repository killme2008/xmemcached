/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.command.binary;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * Auto Discovery config command
 * 
 * @author dennis
 * 
 */
public class BinaryAutoDiscoveryCacheConfigCommand extends BaseBinaryCommand {

  public BinaryAutoDiscoveryCacheConfigCommand(final CountDownLatch latch, String subCommand,
      String key) {
    super(key, ByteUtils.getBytes(key), CommandType.AUTO_DISCOVERY_CONFIG, latch, 0, 0, latch, false, null);
    this.commandType = CommandType.AUTO_DISCOVERY_CONFIG;
    if (subCommand.equals("get")) {
      this.opCode = OpCode.CONFIG_GET;
    } else if (subCommand.equals("set")) {
      this.opCode = OpCode.CONFIG_SET;
    } else if (subCommand.equals("delete")) {
      this.opCode = OpCode.CONFIG_DEL;
    }
  }

  @Override
  protected boolean readValue(ByteBuffer buffer, int bodyLength, int keyLength, int extrasLength) {
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
  protected int getValueLength(CachedData data) {
    return 0;
  }

}
