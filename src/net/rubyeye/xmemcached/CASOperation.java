package net.rubyeye.xmemcached;

public interface CASOperation {
	/**
	 * 最大重试次数
	 * 
	 * @return
	 */
	public int getMaxTries();

	/**
	 * 当失败时回调
	 * 
	 * @param currentCAS
	 * @param currentValue
	 * @return 新的期望设置值
	 */
	public Object getNewValue(long currentCAS, Object currentValue);
}
