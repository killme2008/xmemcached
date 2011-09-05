package net.rubyeye.xmemcached.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.code.yanf4j.core.Session;

import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.networking.MemcachedSession;

/**
 * A round-robin session locator for some special applications,memcacheq or
 * kestrel etc.They doesn't need the same key must always to be stored in same
 * memcached but want to make a cluster.
 * 
 * @author apple
 * 
 */
public class RoundRobinMemcachedSessionLocator implements
		MemcachedSessionLocator {
	private transient volatile List<Session> sessions;
	private AtomicInteger sets = new AtomicInteger(0);

	public Session getSessionByKey(String key) {
		List<Session> copyList = this.sessions;
		if (copyList == null || copyList.isEmpty())
			return null;
		int size = copyList.size();
		return copyList.get(Math.abs(sets.getAndIncrement()) % size);
	}

	public final void updateSessions(final Collection<Session> list) {
		Collection<Session> copySessions = list;
		List<Session> newSessions = new ArrayList<Session>(
				copySessions.size() * 2);
		for (Session session : copySessions) {
			if (session instanceof MemcachedTCPSession) {
				int weight = ((MemcachedSession) session).getWeight();
				for (int i = 0; i < weight; i++) {
					newSessions.add(session);
				}
			} else {
				newSessions.add(session);
			}
		}
		this.sessions = newSessions;
	}

	public void setFailureMode(boolean failureMode) {
		// ignore

	}

}
