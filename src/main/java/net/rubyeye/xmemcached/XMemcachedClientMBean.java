package net.rubyeye.xmemcached;

import java.io.IOException;
import java.util.List;

/**
 * XMemcachedClientMBean，用于动态添加或者移除server
 * 
 * @author dennis
 * 
 */
public interface XMemcachedClientMBean {

	/**
	 * 添加memcached server
	 * 
	 * @param host
	 *            形式如[host1]:[port1] [host2]:[port2] ...形式的服务器列表字符串
	 */
	public void addServer(String hostList) throws IOException;

	/**
	 * 移除memcached server
	 * 
	 * @param host
	 *            形式如[host1]:[port1] [host2]:[port2] ...形式的服务器列表字符串
	 */
	public void removeServer(String hostList);

	/**
	 * 获取服务器列表
	 * 
	 * @return
	 */
	public List<String> getServersDescription();

	/**
	 * get operation timeout setting
	 * 
	 * @return
	 */
	public long getOpTimeout();

	/**
	 * set operation timeout,default is one second.
	 * @param opTimeout
	 */
	public void setOpTimeout(long opTimeout);

}