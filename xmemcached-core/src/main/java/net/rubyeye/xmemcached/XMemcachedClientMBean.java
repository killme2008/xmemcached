package net.rubyeye.xmemcached;

import java.io.IOException;
import java.util.List;

/**
 * XMemcachedClientMBean.It is used for JMX to add/remove memcached server.
 * 
 * @author dennis
 * 
 */
public interface XMemcachedClientMBean {

	/**
	 *Add memcached servers
	 * 
	 * @param host
	 *            a String in the form of
	 *            "[host1]:[port1],[host2]:[port2] [host3]:[port3],[host4]:[port4]"
	 */
	public void addServer(String hostList) throws IOException;

	/**
	 * Add a memcached server
	 * 
	 * @param server
	 *            a String in the form of "[host1]:[port1],[host2]:[port2]"
	 * @param weight
	 *            server's weight
	 */
	public void addOneServerWithWeight(String server, int weight)
			throws IOException;

	/**
	 * Remove memcached servers
	 * 
	 * @param host
	 *            a string in the form of "[host1]:[port1],[host2]:[port2] [host3]:[port3],[host4]:[port4]"
	 */
	public void removeServer(String hostList);

	/**
	 * Get all connected memcached servers
	 * 
	 * @return a list of string,every string is in the form of
	 *         "[host1]:[port1](weight=num1) [host2]:[port2](weight=num1)"
	 */
	public List<String> getServersDescription();

	/**
	 * Set a memcached server's weight
	 * 
	 * @param server
	 * @param weight
	 */
	public void setServerWeight(String server, int weight);

	/**
	 * Return the cache instance name
	 * 
	 * @return
	 */
	public String getName();

}