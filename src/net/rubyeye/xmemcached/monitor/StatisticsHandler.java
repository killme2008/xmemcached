package net.rubyeye.xmemcached.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.rubyeye.xmemcached.command.Command;

public class StatisticsHandler implements StatisticsHandlerMBean {
	private Map<Command.CommandType, AtomicLong> counterMap = new HashMap<Command.CommandType, AtomicLong>();

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
			Map<Command.CommandType, AtomicLong> map = new HashMap<Command.CommandType, AtomicLong>();
			map.put(Command.CommandType.APPEND, new AtomicLong());
			map.put(Command.CommandType.SET, new AtomicLong());
			map.put(Command.CommandType.PREPEND, new AtomicLong());
			map.put(Command.CommandType.CAS, new AtomicLong());
			map.put(Command.CommandType.ADD, new AtomicLong());
			map.put(Command.CommandType.REPLACE, new AtomicLong());
			map.put(Command.CommandType.DELETE, new AtomicLong());
			map.put(Command.CommandType.INCR, new AtomicLong());
			map.put(Command.CommandType.DECR, new AtomicLong());
			map.put(Command.CommandType.GET_HIT, new AtomicLong());
			map.put(Command.CommandType.GET_MSS, new AtomicLong());
			map.put(Command.CommandType.GET_MANY, new AtomicLong());
			map.put(Command.CommandType.GETS_MANY, new AtomicLong());
			this.counterMap = map;
		}
	}

	@Override
	public final boolean isStatistics() {
		return this.statistics;
	}

	public final void statistics(Command.CommandType cmdType) {
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
		return counterMap.get(Command.CommandType.APPEND).get();
	}

	@Override
	public long getCASCount() {
		return counterMap.get(Command.CommandType.CAS).get();
	}

	@Override
	public long getDecrCount() {
		return counterMap.get(Command.CommandType.DECR).get();
	}

	@Override
	public long getDeleteCount() {
		return counterMap.get(Command.CommandType.DELETE).get();
	}

	@Override
	public long getGetHitCount() {
		return counterMap.get(Command.CommandType.GET_HIT).get();
	}

	@Override
	public long getGetMissCount() {
		return counterMap.get(Command.CommandType.GET_MSS).get();
	}

	@Override
	public long getIncrCount() {
		return counterMap.get(Command.CommandType.INCR).get();
	}

	@Override
	public long getMultiGetCount() {
		return counterMap.get(Command.CommandType.GET_MANY).get();
	}

	@Override
	public long getMultiGetsCount() {
		return counterMap.get(Command.CommandType.GETS_MANY).get();
	}

	@Override
	public long getPrependCount() {
		return counterMap.get(Command.CommandType.PREPEND).get();
	}

	@Override
	public long getSetCount() {
		return counterMap.get(Command.CommandType.SET).get();
	}

	@Override
	public long getAddCount() {
		return counterMap.get(Command.CommandType.ADD).get();
	}

	@Override
	public long getReplaceCount() {
		return counterMap.get(Command.CommandType.REPLACE).get();
	}

}
