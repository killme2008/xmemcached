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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * Stats command for text protocol
 * 
 * @author dennis
 * 
 */
public class TextStatsCommand extends Command implements ServerAddressAware {
	public static final ByteBuffer STATS = ByteBuffer.wrap("stats\r\n"
			.getBytes());
	private InetSocketAddress server;
	private String itemName;

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

	public TextStatsCommand(InetSocketAddress server, CountDownLatch latch,
			String itemName) {
		super("stats", (byte[]) null, latch);
		this.commandType = CommandType.STATS;
		this.server = server;
		this.itemName = itemName;
		this.result = new HashMap<String, String>();

	}

	@Override
	@SuppressWarnings("unchecked")
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = null;
		while ((line = ByteUtils.nextLine(buffer)) != null) {
			if (line.equals("END")) { // at the end
				return done(session);
			} else if (line.startsWith("STAT") || line.startsWith("PREFIX")) {
				// Fixed issue 126
				String[] items = line.split(" ");
				((Map<String, String>) getResult()).put(items[1], items[2]);
			} else {
				return decodeError(line);
			}
		}
		return false;
	}

	private final boolean done(MemcachedSession session) {
		countDownLatch();
		return true;
	}

	@Override
	public final void encode() {
		if (this.itemName == null) {
			this.ioBuffer = IoBuffer.wrap(STATS.slice());
		} else {
			this.ioBuffer = IoBuffer.allocate(5 + this.itemName.length() + 3);
			ByteUtils.setArguments(this.ioBuffer, "stats", this.itemName);
			this.ioBuffer.flip();
		}
	}

}
