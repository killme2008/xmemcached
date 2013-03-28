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
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.impl.AbstractMemcachedSessionLocator;

import com.google.code.yanf4j.core.Session;

/**
 * Consistent Hash Algorithm implementation is compatible with libmemcached
 * method.
 * 
 * @author dennis
 * 
 */
public class LibmemcachedMemcachedSessionLocator extends
		AbstractMemcachedSessionLocator {

	static final int DEFAULT_NUM_REPS = 100;
	private transient volatile TreeMap<Long, List<Session>> ketamaSessions = new TreeMap<Long, List<Session>>();
	private volatile int maxTries;
	private int numReps = DEFAULT_NUM_REPS;
	private final Random random = new Random();
	private HashAlgorithm hashAlgorithm = HashAlgorithm.ONE_AT_A_TIME;

	public LibmemcachedMemcachedSessionLocator() {
	}

	public LibmemcachedMemcachedSessionLocator(int numReps,
			HashAlgorithm hashAlgorithm) {
		super();
		this.numReps = numReps;
		this.hashAlgorithm = hashAlgorithm;
	}

	private final void buildMap(Collection<Session> list, HashAlgorithm alg) {
		TreeMap<Long, List<Session>> sessionMap = new TreeMap<Long, List<Session>>();

		for (Session session : list) {
			String sockStr = null;
			if (session.getRemoteSocketAddress().getPort() != 11211) {
				sockStr = session.getRemoteSocketAddress().getHostName() + ":"
						+ session.getRemoteSocketAddress().getPort();
			} else {
				sockStr = session.getRemoteSocketAddress().getHostName();
			}
			for (int i = 0; i < this.numReps; i++) {
				long key = hashAlgorithm.hash(sockStr + "-" + i);
				this.getSessionList(sessionMap, key).add(session);
			}
		}
		this.ketamaSessions = sessionMap;
		this.maxTries = list.size();
	}

	private List<Session> getSessionList(
			TreeMap<Long, List<Session>> sessionMap, long k) {
		List<Session> sessionList = sessionMap.get(k);
		if (sessionList == null) {
			sessionList = new ArrayList<Session>();
			sessionMap.put(k, sessionList);
		}
		return sessionList;
	}

	public final Session getSessionByKey(final String key) {
		if (this.ketamaSessions == null || this.ketamaSessions.size() == 0) {
			return null;
		}
		long hash = hashAlgorithm.hash(key);
		Session rv = this.getSessionByHash(hash);
		int tries = 0;
		while (!this.failureMode && (rv == null || rv.isClosed())
				&& tries++ < this.maxTries) {
			hash = this.nextHash(hash, key, tries);
			rv = this.getSessionByHash(hash);
		}
		return rv;
	}

	public final Session getSessionByHash(final long hash) {
		TreeMap<Long, List<Session>> sessionMap = this.ketamaSessions;
		if (sessionMap.size() == 0) {
			return null;
		}
		Long resultHash = hash;
		if (!sessionMap.containsKey(hash)) {
			// Java 1.6 adds a ceilingKey method, but xmemcached is compatible
			// with jdk5,So use tailMap method to do this.
			SortedMap<Long, List<Session>> tailMap = sessionMap.tailMap(hash);
			if (tailMap.isEmpty()) {
				resultHash = sessionMap.firstKey();
			} else {
				resultHash = tailMap.firstKey();
			}
		}
		//		
		// if (!sessionMap.containsKey(resultHash)) {
		// resultHash = sessionMap.ceilingKey(resultHash);
		// if (resultHash == null && sessionMap.size() > 0) {
		// resultHash = sessionMap.firstKey();
		// }
		// }
		List<Session> sessionList = sessionMap.get(resultHash);
		if (sessionList == null || sessionList.size() == 0) {
			return null;
		}
		int size = sessionList.size();
		return sessionList.get(this.random.nextInt(size));
	}

	public final long nextHash(long hashVal, String key, int tries) {
		long tmpKey = hashAlgorithm.hash(tries + key);
		hashVal += (int) (tmpKey ^ tmpKey >>> 32);
		hashVal &= 0xffffffffL; /* truncate to 32-bits */
		return hashVal;
	}

	public final void updateSessions(final Collection<Session> list) {
		this.buildMap(list, null);
	}
}
