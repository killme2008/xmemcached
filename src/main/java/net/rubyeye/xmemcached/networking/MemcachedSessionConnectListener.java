package net.rubyeye.xmemcached.networking;

import net.rubyeye.xmemcached.MemcachedClient;
/**
 * 
 * @author dennis
 * 
 */
public interface MemcachedSessionConnectListener {

	public void onConnect(MemcachedSession session, MemcachedClient client);

}
