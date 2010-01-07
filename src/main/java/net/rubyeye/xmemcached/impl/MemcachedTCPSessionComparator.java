package net.rubyeye.xmemcached.impl;

import java.util.Comparator;

import com.google.code.yanf4j.core.Session;

public class MemcachedTCPSessionComparator implements Comparator<Session> {

	public int compare(Session o1, Session o2) {
		MemcachedTCPSession session1 = (MemcachedTCPSession) o1;
		MemcachedTCPSession session2 = (MemcachedTCPSession) o2;
		if (session1 == null) {
			return -1;
		}
		if (session2 == null) {
			return 1;
		}
		return session1.getOrder() - session2.getOrder();
	}

}
