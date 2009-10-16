package net.rubyeye.xmemcached.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;

import com.google.code.yanf4j.core.Session;

public class ElectionMemcachedSessionLocator implements MemcachedSessionLocator {

	private transient volatile List<Session> sessions;

	private final HashAlgorithm hashAlgorithm;

	public ElectionMemcachedSessionLocator() {
		hashAlgorithm = HashAlgorithm.ELECTION_HASH;
	}

	public ElectionMemcachedSessionLocator(HashAlgorithm hashAlgorithm) {
		super();
		this.hashAlgorithm = hashAlgorithm;
	}

	public Session getSessionByKey(String key) {
		// copy on write
		List<Session> copySessionList = new ArrayList<Session>(this.sessions);
		Session result = getSessionByElection(key, copySessionList);
		while ((result == null || result.isClosed())
				&& copySessionList.size() > 0) {
			copySessionList.remove(result);
			result = getSessionByElection(key, copySessionList);
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
				MemcachedTCPSession tcpSession = (MemcachedTCPSession) session;
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
