package net.rubyeye.xmemcached.impl;

import java.io.Serializable;
import java.util.Comparator;

import net.rubyeye.xmemcached.networking.MemcachedSession;

import com.google.code.yanf4j.core.Session;

/**
 * Connection comparator,compare with index
 * 
 * @author dennis
 * 
 */
public class MemcachedSessionComparator implements Comparator<Session>,
		Serializable {
	static final long serialVersionUID = -1L;

	public int compare(Session o1, Session o2) {
		MemcachedSession session1 = (MemcachedSession) o1;
		MemcachedSession session2 = (MemcachedSession) o2;
		if (session1 == null) {
			return -1;
		}
		if (session2 == null) {
			return 1;
		}
		return session1.getInetSocketAddressWrapper().getOrder()
				- session2.getInetSocketAddressWrapper().getOrder();
	}

}
