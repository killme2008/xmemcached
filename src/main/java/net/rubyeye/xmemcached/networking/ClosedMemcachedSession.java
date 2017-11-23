package net.rubyeye.xmemcached.networking;

import com.google.code.yanf4j.core.Session;

import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

public interface ClosedMemcachedSession extends Session {

	public void setAllowReconnect(boolean allow);

	public boolean isAllowReconnect();

	public InetSocketAddressWrapper getInetSocketAddressWrapper();

	@Deprecated
	public int getWeight();

	@Deprecated
	public int getOrder();

}
