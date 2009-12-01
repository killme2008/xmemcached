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
package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.Protocol;

public interface CommandFactory {

	/**
	 * set command factory's buffer allocator
	 * 
	 * @since 1.2.0
	 * @param bufferAllocator
	 */
	public void setBufferAllocator(BufferAllocator bufferAllocator);

	/**
	 * 创建delete命令
	 * 
	 * @param key
	 * @param time
	 * @return
	 */
	public abstract Command createDeleteCommand(final String key,
			final byte[] keyBytes, final int time, boolean noreply);

	/**
	 * 创建version command
	 * 
	 * @return
	 */
	public abstract Command createVersionCommand(CountDownLatch latch,
			InetSocketAddress server);

	/**
	 * create flush_all command
	 * 
	 * @return
	 */
	public abstract Command createFlushAllCommand(CountDownLatch latch,
			int delay, boolean noreply);

	/**
	 * create flush_all command
	 * 
	 * @return
	 */
	public abstract Command createStatsCommand(InetSocketAddress server,
			CountDownLatch latch, String itemName);

	/**
	 *创建get,gets命令
	 * 
	 * @param key
	 * @param keyBytes
	 * @param cmdType
	 *            命令类型
	 * @param transcoder TODO
	 * @param cmdBytes
	 *            命令的字节数组，如"get".getBytes()
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public abstract Command createGetCommand(final String key,
			final byte[] keyBytes, final CommandType cmdType, Transcoder transcoder);

	/**
	 * 创建批量获取 command
	 * 
	 * @param <T>
	 * @param keys
	 * @param latch
	 * @param result
	 * @param cmdBytes
	 * @param cmdType
	 * @param transcoder
	 * @return
	 */
	public abstract <T> Command createGetMultiCommand(Collection<String> keys,
			CountDownLatch latch, CommandType cmdType, Transcoder<T> transcoder);

	public abstract Command createIncrDecrCommand(final String key,
			final byte[] keyBytes, final long amount, long initial, int expTime,
			CommandType cmdType, boolean noreply);

	@SuppressWarnings("unchecked")
	public Command createCASCommand(final String key, final byte[] keyBytes,
			final int exp, final Object value, long cas, boolean noreply,
			Transcoder transcoder);

	@SuppressWarnings("unchecked")
	public Command createSetCommand(final String key, final byte[] keyBytes,
			final int exp, final Object value, boolean noreply,
			Transcoder transcoder);

	@SuppressWarnings("unchecked")
	public Command createAddCommand(final String key, final byte[] keyBytes,
			final int exp, final Object value, boolean noreply,
			Transcoder transcoder);

	@SuppressWarnings("unchecked")
	public Command createReplaceCommand(final String key,
			final byte[] keyBytes, final int exp, final Object value,
			boolean noreply, Transcoder transcoder);

	@SuppressWarnings("unchecked")
	public Command createAppendCommand(final String key, final byte[] keyBytes,
			final Object value, boolean noreply, Transcoder transcoder);

	@SuppressWarnings("unchecked")
	public Command createPrependCommand(final String key,
			final byte[] keyBytes, final Object value, boolean noreply,
			Transcoder transcoder);

	/**
	 * Create verbosity command
	 * 
	 * @param latch
	 * @param level
	 * @param noreply
	 * @return
	 */
	public Command createVerbosityCommand(CountDownLatch latch, int level,
			boolean noreply);

	public Protocol getProtocol();
	
}