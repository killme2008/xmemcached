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
package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Quit command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextQuitCommand extends Command {
	public TextQuitCommand() {
		super("quit", (byte[]) null, null);
		commandType = CommandType.QUIT;
	}

	static final IoBuffer QUIT = IoBuffer.wrap("quit\r\n".getBytes());

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		// do nothing
		return true;
	}

	@Override
	public final void encode() {
		ioBuffer = QUIT.slice();
	}

}
