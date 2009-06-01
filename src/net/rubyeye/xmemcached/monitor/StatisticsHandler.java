package net.rubyeye.xmemcached.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.xmemcached.command.CommandType;

public class StatisticsHandler implements StatisticsHandlerMBean {
	private Map<CommandType, AtomicLong> counterMap = new HashMap<CommandType, AtomicLong>();

	public StatisticsHandler() {
		buildCounterMap();
		XMemcachedMbeanServer.getInstance().registMBean(
				this,
				this.getClass().getPackage().getName() + ":type="
						+ this.getClass().getSimpleName());
	}

	private boolean statistics = Boolean.valueOf(System.getProperty(
			Constants.XMEMCACHED_STATISTICS_ENABLE, "false"));

	private void buildCounterMap() {
		if (statistics) {
			Map<CommandType, AtomicLong> map = new HashMap<CommandType, AtomicLong>();
			map.put(CommandType.APPEND, new AtomicLong());
			map.put(CommandType.SET, new AtomicLong());
			map.put(CommandType.PREPEND, new AtomicLong());
			map.put(CommandType.CAS, new AtomicLong());
			map.put(CommandType.ADD, new AtomicLong());
			map.put(CommandType.REPLACE, new AtomicLong());
			map.put(CommandType.DELETE, new AtomicLong());
			map.put(CommandType.INCR, new AtomicLong());
			map.put(CommandType.DECR, new AtomicLong());
			map.put(CommandType.GET_HIT, new AtomicLong());
			map.put(CommandType.GET_MSS, new AtomicLong());
			map.put(CommandType.GET_MANY, new AtomicLong());
			map.put(CommandType.GETS_MANY, new AtomicLong());
			this.counterMap = map;
		}
	}

	@Override
	public final boolean isStatistics() {
		return this.statistics;
	}

	public final void statistics(CommandType cmdType) {
		if (this.statistics)
			this.counterMap.get(cmdType).incrementAndGet();
	}

	@Override
	public final void setStatistics(boolean statistics) {
		this.statistics = statistics;
		buildCounterMap();

	}

	@Override
	public long getAppendCount() {
		return counterMap.get(CommandType.APPEND).get();
	}

	@Override
	public long getCASCount() {
		return counterMap.get(CommandType.CAS).get();
	}

	@Override
	public long getDecrCount() {
		return counterMap.get(CommandType.DECR).get();
	}

	@Override
	public long getDeleteCount() {
		return counterMap.get(CommandType.DELETE).get();
	}

	@Override
	public long getGetHitCount() {
		return counterMap.get(CommandType.GET_HIT).get();
	}

	@Override
	public long getGetMissCount() {
		return counterMap.get(CommandType.GET_MSS).get();
	}

	@Override
	public long getIncrCount() {
		return counterMap.get(CommandType.INCR).get();
	}

	@Override
	public long getMultiGetCount() {
		return counterMap.get(CommandType.GET_MANY).get();
	}

	@Override
	public long getMultiGetsCount() {
		return counterMap.get(CommandType.GETS_MANY).get();
	}

	@Override
	public long getPrependCount() {
		return counterMap.get(CommandType.PREPEND).get();
	}

	@Override
	public long getSetCount() {
		return counterMap.get(CommandType.SET).get();
	}

	@Override
	public long getAddCount() {
		return counterMap.get(CommandType.ADD).get();
	}

	@Override
	public long getReplaceCount() {
		return counterMap.get(CommandType.REPLACE).get();
	}

}
