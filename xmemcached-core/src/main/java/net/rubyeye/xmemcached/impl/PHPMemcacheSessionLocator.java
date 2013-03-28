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
import net.rubyeye.xmemcached.networking.MemcachedSession;

import com.google.code.yanf4j.core.Session;

/**
 * Session locator base on hash(key) mod sessions.size(). Uses the PHP
 * memcached hash strategy so it's easier to share data with PHP based
 * clients.
 *
 * @author aravind
 *
 */
public class PHPMemcacheSessionLocator extends
		AbstractMemcachedSessionLocator {

	private HashAlgorithm hashAlgorithm;
	private transient volatile List<Session> sessions;

	public PHPMemcacheSessionLocator() {
		this.hashAlgorithm = HashAlgorithm.NATIVE_HASH;
	}

	public PHPMemcacheSessionLocator(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

	public final void setHashAlgorighm(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

	public final long getHash(int size, String key) {
		long hash = this.hashAlgorithm.hash(key);
    hash = (hash >> 16) & 0x7fff;
		return hash % size;
	}

	public final Session getSessionByKey(final String key) {
		if (this.sessions == null || this.sessions.size() == 0) {
			return null;
		}
		// Copy on read
		List<Session> sessionList = this.sessions;
		int size = sessionList.size();
		if (size == 0) {
			return null;
		}
		long start = this.getHash(size, key);
		Session session = sessionList.get((int) start);
		// If it is not failure mode,get next available session
		if (!this.failureMode && (session == null || session.isClosed())) {
			long next = this.getNext(size, start);
			while ((session == null || session.isClosed()) && next != start) {
				session = sessionList.get((int) next);
				next = this.getNext(size, next);
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
}
