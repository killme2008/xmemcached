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
package net.rubyeye.xmemcached.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Future;

import net.rubyeye.xmemcached.FlowControl;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

import com.google.code.yanf4j.core.Controller;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;

/**
 * Connector which is used to connect to memcached server.
 * 
 * @author dennis
 * 
 */
public interface Connector extends Controller {
	public void setOptimizeMergeBuffer(boolean optimiezeMergeBuffer);

	public void setMergeFactor(int factor);

	public void setOptimizeGet(boolean optimizeGet);

	public void removeSession(Session session);

	public Queue<Session> getSessionByAddress(InetSocketAddress address);

	public List<Session> getStandbySessionListByMainNodeAddr(
			InetSocketAddress address);

	public Set<Session> getSessionSet();

	public void setHealSessionInterval(long interval);

	public long getHealSessionInterval();

	public Session send(Command packet) throws MemcachedException;

	public void setConnectionPoolSize(int connectionPoolSize);

	public void setBufferAllocator(BufferAllocator bufferAllocator);

	public void removeReconnectRequest(InetSocketAddress address);

	public void setEnableHealSession(boolean enableHealSession);

	public void addToWatingQueue(ReconnectRequest request);

	@SuppressWarnings("unchecked")
	public void setSocketOptions(Map<SocketOption, Object> options);

	public Future<Boolean> connect(InetSocketAddressWrapper addressWrapper)
			throws IOException;

	public void updateSessions();

	public void setSessionLocator(MemcachedSessionLocator sessionLocator);

	/**
	 * Make all connection sending a quit command to memcached
	 */
	public void quitAllSessions();

	public Queue<ReconnectRequest> getReconnectRequestQueue();

	public void setFailureMode(boolean failureMode);

	/**
	 * Returns the noreply operations flow control manager.
	 * 
	 * @return
	 */
	public FlowControl getNoReplyOpsFlowControl();
}
