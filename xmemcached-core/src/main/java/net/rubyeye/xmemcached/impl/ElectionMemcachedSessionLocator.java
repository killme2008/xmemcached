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
 * Election hash strategy
 * 
 * @author dennis
 * 
 */
public class ElectionMemcachedSessionLocator extends
		AbstractMemcachedSessionLocator {

	private transient volatile List<Session> sessions;

	private final HashAlgorithm hashAlgorithm;

	public ElectionMemcachedSessionLocator() {
		this.hashAlgorithm = HashAlgorithm.ELECTION_HASH;
	}

	public ElectionMemcachedSessionLocator(HashAlgorithm hashAlgorithm) {
		super();
		this.hashAlgorithm = hashAlgorithm;
	}

	public Session getSessionByKey(String key) {
		// copy on write
		List<Session> copySessionList = new ArrayList<Session>(this.sessions);
		Session result = this.getSessionByElection(key, copySessionList);
		while (!this.failureMode && (result == null || result.isClosed())
				&& copySessionList.size() > 0) {
			copySessionList.remove(result);
			result = this.getSessionByElection(key, copySessionList);
		}
		return result;
	}

	private Session getSessionByElection(String key,
			List<Session> copySessionList) {
		Session result = null;
		long highScore = 0;
		for (Session session : copySessionList) {
			long hash = 0;
			if (session instanceof MemcachedTCPSession) {
				MemcachedSession tcpSession = (MemcachedSession) session;
				for (int i = 0; i < tcpSession.getWeight(); i++) {
					hash = this.hashAlgorithm.hash(session
							.getRemoteSocketAddress().toString()
							+ "-" + i + key);
					if (hash > highScore) {
						highScore = hash;
						result = session;
					}
				}
			} else {
				hash = this.hashAlgorithm.hash(session.getRemoteSocketAddress()
						.toString()
						+ key);

			}
			if (hash > highScore) {
				highScore = hash;
				result = session;
			}
		}
		return result;
	}

	public void updateSessions(Collection<Session> list) {
		this.sessions = new ArrayList<Session>(list);
	}

}
