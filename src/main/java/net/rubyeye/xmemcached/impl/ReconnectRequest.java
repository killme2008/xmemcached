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
package net.rubyeye.xmemcached.impl;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

/**
 * Auto reconnect request
 * 
 * @author dennis
 * 
 */
public final class ReconnectRequest implements Delayed {

	private InetSocketAddressWrapper inetSocketAddressWrapper;
	private int tries;

	private int weight;

	private static final long MIN_RECONNECT_INTERVAL = 1000;

	private static final long MAX_RECONNECT_INTERVAL = 60 * 1000;

	private volatile long nextReconnectTimestamp;

	public ReconnectRequest(InetSocketAddressWrapper inetSocketAddressWrapper,
			int tries, int weight, long reconnectInterval) {
		super();
		setInetSocketAddressWrapper(inetSocketAddressWrapper);
		setTries(tries); // record reconnect times
		this.weight = weight;
		reconnectInterval = normalInterval(reconnectInterval);
		nextReconnectTimestamp = System.currentTimeMillis() + reconnectInterval;
	}

	private long normalInterval(long reconnectInterval) {
		if (reconnectInterval < MIN_RECONNECT_INTERVAL)
			reconnectInterval = MIN_RECONNECT_INTERVAL;
		if (reconnectInterval > MAX_RECONNECT_INTERVAL) {
			reconnectInterval = MAX_RECONNECT_INTERVAL;
		}
		return reconnectInterval;
	}

	public long getDelay(TimeUnit unit) {
		return nextReconnectTimestamp - System.currentTimeMillis();
	}

	public int compareTo(Delayed o) {
		ReconnectRequest other = (ReconnectRequest) o;
		if (nextReconnectTimestamp > other.nextReconnectTimestamp)
			return 1;
		else
			return -1;
	}

	public final InetSocketAddressWrapper getInetSocketAddressWrapper() {
		return inetSocketAddressWrapper;
	}

	public void updateNextReconnectTimeStamp(long interval) {
		interval = normalInterval(interval);
		nextReconnectTimestamp = System.currentTimeMillis() + interval;
	}

	public final void setInetSocketAddressWrapper(
			InetSocketAddressWrapper inetSocketAddressWrapper) {
		this.inetSocketAddressWrapper = inetSocketAddressWrapper;
	}

	public final void setTries(int tries) {
		this.tries = tries;
	}

	public final int getTries() {
		return tries;
	}

	public final int getWeight() {
		return weight;
	}

	public final void setWeight(int weight) {
		this.weight = weight;
	}

}