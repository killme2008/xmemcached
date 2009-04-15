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

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.MemcachedTCPSession;

/**
 * 基于余数的分布查找
 *
 * @author dennis
 *
 */
public class ArrayMemcachedSessionLocator implements MemcachedSessionLocator {

	private HashAlgorithm hashAlgorighm;
	private List<MemcachedTCPSession> sessions;

	public ArrayMemcachedSessionLocator() {
		this.hashAlgorighm = HashAlgorithm.NATIVE_HASH;
	}

	public ArrayMemcachedSessionLocator(HashAlgorithm hashAlgorighm) {
		this.hashAlgorighm = hashAlgorighm;
	}

	public final long getHash(int size, String key) {
		long hash = hashAlgorighm.hash(key);
		return hash % size;
	}

	@Override
	public final Session getSessionByKey(final String key) {
		List<MemcachedTCPSession> sessionList = sessions;
		int size = sessionList.size();
		if (size == 0) {
			return null;
		}
		long start = getHash(size, key);
		MemcachedTCPSession session = sessionList.get((int) start);
		long next = getNext(size, start);
		while ((session == null || session.isClose()) && next != start) {
			session = sessionList.get((int) next);
			next = getNext(size, next);
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

	@Override
	public final void updateSessionList(final List<MemcachedTCPSession> list) {
		sessions = list;
	}
}
