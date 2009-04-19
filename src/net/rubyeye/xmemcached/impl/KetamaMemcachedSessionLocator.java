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

import com.google.code.yanf4j.nio.Session;
import java.util.List;
import java.util.TreeMap;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.MemcachedTCPSession;

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
 * 一致性hash算法，基于TreeMap，直接从spymemcached挪用
 *
 * @author dennis
 *
 */
public class KetamaMemcachedSessionLocator implements MemcachedSessionLocator {

	static final int NUM_REPS = 160;
	private transient volatile TreeMap<Long, MemcachedTCPSession> ketamaSessions = new TreeMap<Long, MemcachedTCPSession>();
	private final HashAlgorithm hashAlg;
	private volatile int maxTries;

	public KetamaMemcachedSessionLocator() {
		this.hashAlg = HashAlgorithm.KETAMA_HASH;
	}

	public KetamaMemcachedSessionLocator(HashAlgorithm alg) {
		this.hashAlg = alg;
	}

	public KetamaMemcachedSessionLocator(List<MemcachedTCPSession> list,
			HashAlgorithm alg) {
		super();
		hashAlg = alg;
		buildMap(list, alg);
	}

	private final void buildMap(List<MemcachedTCPSession> list,
			HashAlgorithm alg) {
		TreeMap<Long, MemcachedTCPSession> sessionMap = new TreeMap<Long, MemcachedTCPSession>();

		for (MemcachedTCPSession session : list) {
			String sockStr = String.valueOf(session.getRemoteSocketAddress());
			/**
			 * 按照spy作者的说法，这里是对KETAMA_HASH做特殊处理，为了复用chunk，具体我并不明白，暂时直接拷贝
			 */
			if (alg == HashAlgorithm.KETAMA_HASH) {
				for (int i = 0; i < NUM_REPS / 4; i++) {
					byte[] digest = HashAlgorithm.computeMd5(sockStr + "-" + i);
					for (int h = 0; h < 4; h++) {
						Long k = ((long) (digest[3 + h * 4] & 0xFF) << 24)
								| ((long) (digest[2 + h * 4] & 0xFF) << 16)
								| ((long) (digest[1 + h * 4] & 0xFF) << 8)
								| (digest[h * 4] & 0xFF);
						sessionMap.put(k, session);
					}

				}
			} else {
				for (int i = 0; i < NUM_REPS; i++) {
					sessionMap.put(alg.hash(sockStr + "-" + i), session);
				}
			}
		}
		ketamaSessions = sessionMap;
		this.maxTries = list.size();
	}

	@Override
	public final Session getSessionByKey(final String key) {
		Long hash = hashAlg.hash(key);
		MemcachedTCPSession rv = getSessionByHash(hash);
		int tries = 0;
		while ((rv == null || rv.isClosed()) && tries++ < this.maxTries) {
			hash = nextHash(hash, key, tries);
			rv = getSessionByHash(hash);
		}
		return rv;
	}

	private final MemcachedTCPSession getSessionByHash(final Long hash) {
		TreeMap<Long, MemcachedTCPSession> sessionMap = ketamaSessions;
		Long resultHash = hash;
		if (!sessionMap.containsKey(resultHash)) {
			resultHash = sessionMap.ceilingKey(resultHash);
			if (resultHash == null) {
				resultHash = sessionMap.firstKey();
			}
		}

		return sessionMap.get(resultHash);
	}

	private final long nextHash(long hashVal, String key, int tries) {
		long tmpKey = hashAlg.hash(tries + key);
		hashVal += (int) (tmpKey ^ (tmpKey >>> 32));
		hashVal &= 0xffffffffL; /* truncate to 32-bits */
		return hashVal;
	}

	@Override
	public final void updateSessionList(final List<MemcachedTCPSession> list) {
		buildMap(list, this.hashAlg);
	}
}
