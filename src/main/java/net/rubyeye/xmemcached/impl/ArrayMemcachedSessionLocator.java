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
import java.util.List;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;

import com.google.code.yanf4j.core.Session;

/**
 * Session locator base on hash(key) mod sessions.size().Standard hash strategy
 * 
 * @author dennis
 * 
 */
public class ArrayMemcachedSessionLocator implements MemcachedSessionLocator {

	private HashAlgorithm hashAlgorighm;
	private transient volatile List<Session> sessions;

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

	
	public final Session getSessionByKey(final String key) {
		if (this.sessions == null || this.sessions.size() == 0) {
			return null;
		}
		List<Session> sessionList = this.sessions;
		int size = sessionList.size();
		if (size == 0) {
			return null;
		}
		long start = getHash(size, key);
		Session session = sessionList.get((int) start);
		if (session == null || session.isClosed()) {
			long next = getNext(size, start);
			while ((session == null || session.isClosed()) && next != start) {
				session = sessionList.get((int) next);
				next = getNext(size, next);
			}
		}
		return session;
	}

	public final long getNext(int size, long start) {
		if (start == size - 1) {
			return 0;
		} else {
			return start + 1;
		}
	}

	
	public final void updateSessions(final Collection<Session> list) {
		Collection<Session> copySessions = list;
		List<Session> newSessions = new ArrayList<Session>(
				copySessions.size() * 2);
		for (Session session : copySessions) {
			if (session instanceof MemcachedTCPSession) {
				int weight = ((MemcachedTCPSession) session).getWeight();
				for (int i = 0; i < weight; i++) {
					newSessions.add(session);
				}
			} else {
				newSessions.add(session);
			}
		}
		this.sessions = newSessions;
	}
}
