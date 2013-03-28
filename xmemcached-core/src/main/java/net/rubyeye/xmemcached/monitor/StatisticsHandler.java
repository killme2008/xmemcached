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
package net.rubyeye.xmemcached.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.xmemcached.command.CommandType;

/**
 * Statistics helper
 * 
 * @author dennis
 * 
 */
public class StatisticsHandler implements StatisticsHandlerMBean {
	private Map<CommandType, AtomicLong> counterMap = new HashMap<CommandType, AtomicLong>();

	public StatisticsHandler() {
		buildCounterMap();
		XMemcachedMbeanServer.getInstance().registMBean(
				this,
				this.getClass().getPackage().getName() + ":type="
						+ this.getClass().getSimpleName() + "-"
						+ MemcachedClientNameHolder.getName());
	}

	private boolean statistics = Boolean.valueOf(System.getProperty(
			Constants.XMEMCACHED_STATISTICS_ENABLE, "false"));

	private void buildCounterMap() {
		if (this.statistics) {
			Map<CommandType, AtomicLong> map = new HashMap<CommandType, AtomicLong>();
			map.put(CommandType.APPEND, new AtomicLong());
			map.put(CommandType.SET, new AtomicLong());
			map.put(CommandType.SET_MANY, new AtomicLong());
			map.put(CommandType.PREPEND, new AtomicLong());
			map.put(CommandType.CAS, new AtomicLong());
			map.put(CommandType.ADD, new AtomicLong());
			map.put(CommandType.REPLACE, new AtomicLong());
			map.put(CommandType.DELETE, new AtomicLong());
			map.put(CommandType.INCR, new AtomicLong());
			map.put(CommandType.DECR, new AtomicLong());
			map.put(CommandType.GET_HIT, new AtomicLong());
			map.put(CommandType.GET_MISS, new AtomicLong());
			map.put(CommandType.GET_MANY, new AtomicLong());
			map.put(CommandType.GETS_MANY, new AtomicLong());
			this.counterMap = map;
		}
	}

	public final boolean isStatistics() {
		return this.statistics;
	}

	public final void statistics(CommandType cmdType) {
		if (this.statistics && this.counterMap.get(cmdType) != null) {
			this.counterMap.get(cmdType).incrementAndGet();
		}
	}

	public final void statistics(CommandType cmdType, int count) {
		if (this.statistics && this.counterMap.get(cmdType) != null) {
			this.counterMap.get(cmdType).addAndGet(count);
		}
	}

	public final void setStatistics(boolean statistics) {
		this.statistics = statistics;
		buildCounterMap();

	}

	public void resetStats() {
		if (this.statistics) {
			buildCounterMap();
		}
	}

	public long getAppendCount() {
		return this.counterMap.get(CommandType.APPEND).get();
	}

	public long getCASCount() {
		return this.counterMap.get(CommandType.CAS).get();
	}

	public long getDecrCount() {
		return this.counterMap.get(CommandType.DECR).get();
	}

	public long getDeleteCount() {
		return this.counterMap.get(CommandType.DELETE).get();
	}

	public long getGetHitCount() {
		return this.counterMap.get(CommandType.GET_HIT).get();
	}

	public long getGetMissCount() {
		return this.counterMap.get(CommandType.GET_MISS).get();
	}

	public long getIncrCount() {
		return this.counterMap.get(CommandType.INCR).get();
	}

	public long getMultiGetCount() {
		return this.counterMap.get(CommandType.GET_MANY).get();
	}

	public long getMultiGetsCount() {
		return this.counterMap.get(CommandType.GETS_MANY).get();
	}

	public long getPrependCount() {
		return this.counterMap.get(CommandType.PREPEND).get();
	}

	public long getSetCount() {
		return this.counterMap.get(CommandType.SET).get()
				+ this.counterMap.get(CommandType.SET_MANY).get();
	}

	public long getAddCount() {
		return this.counterMap.get(CommandType.ADD).get();
	}

	public long getReplaceCount() {
		return this.counterMap.get(CommandType.REPLACE).get();
	}

}
