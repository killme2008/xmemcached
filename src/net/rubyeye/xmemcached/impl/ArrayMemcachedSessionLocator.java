package net.rubyeye.xmemcached.impl;

import java.util.List;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.MemcachedTCPSession;

public class ArrayMemcachedSessionLocator implements MemcachedSessionLocator {
	protected HashAlgorithm hashAlgorighm;
	List<MemcachedTCPSession> sessions;

	public ArrayMemcachedSessionLocator() {
		this.hashAlgorighm = HashAlgorithm.NATIVE_HASH;
	}

	public ArrayMemcachedSessionLocator(HashAlgorithm hashAlgorighm) {
		this.hashAlgorighm = hashAlgorighm;
	}

	@Override
	public long getIndexByKey(String key) {
		long hash = hashAlgorighm.hash(key);
		return hash % sessions.size();
	}

	@Override
	public MemcachedTCPSession getSessionByKey(String key) {
		if (sessions.size() == 0)
			return null;
		long mod = getIndexByKey(key);
		int findCount = 0;
		Label: while (findCount < 10) {
			MemcachedTCPSession session = sessions.get((int) mod);
			findCount++;
			session = sessions.get((int) mod);
			if (session == null || session.isClose()) {
				mod = (mod > sessions.size() - 1) ? 0 : mod + 1;
				break Label;
			} else
				return session;
		}
		return null;
	}

	@Override
	public void setSessionList(List<MemcachedTCPSession> list) {
		sessions = list;
	}
}
