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
	 *            a String in the form of "[host1]:[port1] [host2]:[port2]"
	 */
	public void addServer(String hostList) throws IOException;

	/**
	 * Add a memcached server
	 * 
	 * @param server
	 *            a String in the form of "[host1]:[port1]"
	 * @param weight
	 *            server's weight
	 */
	public void addOneServerWithWeight(String server, int weight)
			throws IOException;

	/**
	 * Remove memcached servers
	 * 
	 * @param host
	 *            a string in the form of "[host1]:[port1] [host2]:[port2]"
	 */
	public void removeServer(String hostList);

	/**
	 * Get all connected memcached servers
	 * 
	 * @return a list of string,every string is in the form of
	 *         "[host1]:[port1] [host2]:[port2]"
	 */
	public List<String> getServersDescription();

}