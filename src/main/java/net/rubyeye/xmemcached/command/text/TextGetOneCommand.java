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
package net.rubyeye.xmemcached.command.text;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

/**
 * Get command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextGetOneCommand extends TextGetCommand {

  public TextGetOneCommand(String key, byte[] keyBytes, CommandType cmdType, CountDownLatch latch) {
    super(key, keyBytes, cmdType, latch);
  }

  @Override
  public void dispatch() {
    if (this.mergeCount < 0) {
      // single get
      if (this.returnValues.get(this.getKey()) == null) {
        if (!this.wasFirst) {
          decodeError();
        } else {
          this.countDownLatch();
        }
      } else {
        CachedData data = this.returnValues.get(this.getKey());
        setResult(data);
        this.countDownLatch();
      }
    } else {
      // merge get
      // Collection<Command> mergeCommands = mergeCommands.values();
      getIoBuffer().free();
      for (Command nextCommand : mergeCommands.values()) {
        TextGetCommand textGetCommand = (TextGetCommand) nextCommand;
        textGetCommand.countDownLatch();
        if (textGetCommand.assocCommands != null) {
          for (Command assocCommand : textGetCommand.assocCommands) {
            assocCommand.countDownLatch();
          }
        }
      }
    }
  }
}
