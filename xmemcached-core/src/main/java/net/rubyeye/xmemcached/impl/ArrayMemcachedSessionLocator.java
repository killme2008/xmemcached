/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.networking.MemcachedSession;

import com.google.code.yanf4j.core.Session;

/**
 * Session locator base on hash(key) mod sessions.size().Standard hash strategy
 * 
 * @author dennis
 * 
 */
public class ArrayMemcachedSessionLocator extends
		AbstractMemcachedSessionLocator {

	private HashAlgorithm hashAlgorighm;
	private transient volatile List<List<Session>> sessions;

	public ArrayMemcachedSessionLocator() {
		this.hashAlgorighm = HashAlgorithm.NATIVE_HASH;
	}

	public ArrayMemcachedSessionLocator(HashAlgorithm hashAlgorighm) {
		this.hashAlgorighm = hashAlgorighm;
	}

	public final void setHashAlgorighm(HashAlgorithm hashAlgorighm) {
		this.hashAlgorighm = hashAlgorighm;
	}

	public final long getHash(int size, String key) {
		long hash = this.hashAlgorighm.hash(key);
		return hash % size;
	}

	final Random rand = new Random();

	public final Session getSessionByKey(final String key) {
		if (this.sessions == null || this.sessions.size() == 0) {
			return null;
		}
		// Copy on read
		List<List<Session>> sessionList = this.sessions;
		int size = sessionList.size();
		if (size == 0) {
			return null;
		}
		long start = this.getHash(size, key);
		List<Session> sessions = sessionList.get((int) start);
		Session session = getRandomSession(sessions);

		// If it is not failure mode,get next available session
		if (!this.failureMode && (session == null || session.isClosed())) {
			long next = this.getNext(size, start);
			while ((session == null || session.isClosed()) && next != start) {
				sessions = sessionList.get((int) next);
				next = this.getNext(size, next);
				session = getRandomSession(sessions);
			}
		}
		return session;
	}

	private Session getRandomSession(List<Session> sessions) {
		if (sessions == null || sessions.isEmpty())
			return null;
		return sessions.get(rand.nextInt(sessions.size()));
	}

	public final long getNext(int size, long start) {
		if (start == size - 1) {
			return 0;
		} else {
			return start + 1;
		}
	}

	public final void updateSessions(final Collection<Session> list) {
		if (list == null || list.isEmpty()) {
			this.sessions = Collections.emptyList();
			return;
		}
		Collection<Session> copySessions = list;
		List<List<Session>> tmpList = new ArrayList<List<Session>>();
		Session target = null;
		List<Session> subList = null;
		for (Session session : copySessions) {
			if (target == null) {
				target = session;
				subList = new ArrayList<Session>();
				subList.add(target);
			} else {
				if (session.getRemoteSocketAddress().equals(
						target.getRemoteSocketAddress())) {
					subList.add(session);
				} else {					
					tmpList.add(subList);
					target = session;
					subList = new ArrayList<Session>();
					subList.add(target);
				}
			}
		}

		// The last one
		if (subList != null) {
			tmpList.add(subList);
		}

		List<List<Session>> newSessions = new ArrayList<List<Session>>(
				tmpList.size() * 2);
		for (List<Session> sessions : tmpList) {
			if (sessions != null && !sessions.isEmpty()) {
				Session session = sessions.get(0);
				if (session instanceof MemcachedTCPSession) {
					int weight = ((MemcachedSession) session).getWeight();
					for (int i = 0; i < weight; i++) {
						newSessions.add(sessions);
					}
				} else {
					newSessions.add(sessions);
				}
			}

		}
		this.sessions = newSessions;
	}
}
