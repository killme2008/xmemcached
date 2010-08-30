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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.networking.MemcachedSession;

import com.google.code.yanf4j.core.Session;

/**
 * ConnectionFactory instance that sets up a ketama compatible connection.
 *
 * <p>
 * This implementation piggy-backs on the functionality of the
 * <code>DefaultConnectionFactory</code> in terms of connections and queue
 * handling. Where it differs is that it uses both the <code>
 * KetamaNodeLocator</code>
 * and the <code>HashAlgorithm.KETAMA_HASH</code> to provide consistent node
 * hashing.
 *
 * @see http://www.last.fm/user/RJ/journal/2007/04/10/392555/
 *
 * </p>
 */
/**
 * Consistent Hash Algorithm implementation,based on TreeMap.tailMap(hash)
 * method.
 * 
 * @author dennis
 * 
 */
public class KetamaMemcachedSessionLocator implements MemcachedSessionLocator {

	static final int NUM_REPS = 160;
	private transient volatile TreeMap<Long, List<Session>> ketamaSessions = new TreeMap<Long, List<Session>>();
	private final HashAlgorithm hashAlg;
	private volatile int maxTries;
	private final Random random = new SecureRandom();

	public KetamaMemcachedSessionLocator() {
		hashAlg = HashAlgorithm.KETAMA_HASH;
	}

	public KetamaMemcachedSessionLocator(HashAlgorithm alg) {
		hashAlg = alg;
	}

	public KetamaMemcachedSessionLocator(List<Session> list, HashAlgorithm alg) {
		super();
		hashAlg = alg;
		buildMap(list, alg);
	}

	private final void buildMap(Collection<Session> list, HashAlgorithm alg) {
		TreeMap<Long, List<Session>> sessionMap = new TreeMap<Long, List<Session>>();

		for (Session session : list) {
			String sockStr = String.valueOf(session.getRemoteSocketAddress());
			/**
			 * Duplicate 160 X weight references
			 */
			int numReps = NUM_REPS;
			if (session instanceof MemcachedTCPSession) {
				numReps *= ((MemcachedSession) session).getWeight();
			}
			if (alg == HashAlgorithm.KETAMA_HASH) {
				for (int i = 0; i < numReps / 4; i++) {
					byte[] digest = HashAlgorithm.computeMd5(sockStr + "-" + i);
					for (int h = 0; h < 4; h++) {
						long k = (long) (digest[3 + h * 4] & 0xFF) << 24
								| (long) (digest[2 + h * 4] & 0xFF) << 16
								| (long) (digest[1 + h * 4] & 0xFF) << 8
								| digest[h * 4] & 0xFF;
						getSessionList(sessionMap, k).add(session);
					}

				}
			} else {
				for (int i = 0; i < numReps; i++) {
					long key = alg.hash(sockStr + "-" + i);
					getSessionList(sessionMap, key).add(session);
				}
			}
		}
		ketamaSessions = sessionMap;
		maxTries = list.size();
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
		if (ketamaSessions == null || ketamaSessions.size() == 0) {
			return null;
		}
		long hash = hashAlg.hash(key);
		Session rv = getSessionByHash(hash);
		int tries = 0;
		while ((rv == null || rv.isClosed()) && tries++ < maxTries) {
			hash = nextHash(hash, key, tries);
			rv = getSessionByHash(hash);
		}
		return rv;
	}

	public final Session getSessionByHash(final long hash) {
		TreeMap<Long, List<Session>> sessionMap = ketamaSessions;
		if (sessionMap.size() == 0)
			return null;
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
		if (sessionList == null || sessionList.size() == 0)
			return null;
		int size = sessionList.size();
		return sessionList.get(random.nextInt(size));
	}

	public final long nextHash(long hashVal, String key, int tries) {
		long tmpKey = hashAlg.hash(tries + key);
		hashVal += (int) (tmpKey ^ tmpKey >>> 32);
		hashVal &= 0xffffffffL; /* truncate to 32-bits */
		return hashVal;
	}

	public final void updateSessions(final Collection<Session> list) {
		buildMap(list, hashAlg);
	}
}
