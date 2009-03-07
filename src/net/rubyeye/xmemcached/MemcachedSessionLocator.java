package net.rubyeye.xmemcached;

import java.util.List;

public interface MemcachedSessionLocator {
	public MemcachedTCPSession getSessionByKey(String key);

	public long getIndexByKey(String key);

	public void setSessionList(List<MemcachedTCPSession> list);
}
