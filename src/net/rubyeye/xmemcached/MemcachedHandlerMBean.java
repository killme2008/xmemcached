package net.rubyeye.xmemcached;

public interface MemcachedHandlerMBean {
	public long getGetHitCount();

	public long getGetMissCount();

	public long getSetCount();

	public long getAppendCount();

	public long getPrependCount();

	public long getCASCount();

	public long getDeleteCount();

	public long getIncrCount();

	public long getDecrCount();

	public long getMultiGetCount();

	public long getMultiGetsCount();

	public long getAddCount();

	public long getReplaceCount();

	/**
	 * 是否开启统计
	 * 
	 * @return
	 */
	public boolean isStatistics();

	/**
	 * 开启或者关闭统计
	 * 
	 * @param statistics
	 */
	public void setStatistics(boolean statistics);

}
