package net.rubyeye.xmemcached.impl;

import java.util.Comparator;

import net.rubyeye.xmemcached.networking.MemcachedSession;

import com.google.code.yanf4j.core.Session;

public class MemcachedSessionComparator implements Comparator<Session> {

	public int compare(Session o1, Session o2) {
		MemcachedSession session1 = (MemcachedSession) o1;
		MemcachedSession session2 = (MemcachedSession) o2;
		if (session1 == null) {
			return -1;
		}
		if (session2 == null) {
			return 1;
		}
		return session1.getOrder() - session2.getOrder();
	}

}
