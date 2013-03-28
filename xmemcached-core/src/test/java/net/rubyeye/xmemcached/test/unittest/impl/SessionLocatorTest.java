package net.rubyeye.xmemcached.test.unittest.impl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.test.unittest.MockSession;

import com.google.code.yanf4j.core.Session;

public class SessionLocatorTest extends TestCase {
	MemcachedSessionLocator sessionLocator;

	public void testArraySessionLocator() {
		sessionLocator = new ArrayMemcachedSessionLocator();

		List<Session> sessions = new ArrayList<Session>();
		for (int i = 8080; i < 8100; i++) {
			sessions.add(new MockSession(i));
		}
		sessionLocator.updateSessions(sessions);
		for (int i = 1; i <= 10; i++) {
			String key = String.valueOf(i);
			int mod = key.hashCode() % sessions.size();
			assertSame(sessions.get(mod), sessionLocator.getSessionByKey(key));
		}

		String key = "test";
		int oldIndex = key.hashCode() % sessions.size();
		Session oldSession = sessions.get(oldIndex);

		assertSame(oldSession, sessionLocator.getSessionByKey(key));
		// close old session
		oldSession.close();
		assertNotSame(oldSession, sessionLocator.getSessionByKey(key));
		// use next
		assertSame(sessions.get(oldIndex + 1), sessionLocator
				.getSessionByKey(key));
		sessions = new ArrayList<Session>();
		sessionLocator.updateSessions(sessions);
		assertNull(sessionLocator.getSessionByKey(key));
	}

	public void testKetamaMemcachedSessionLocator() {
		sessionLocator = new KetamaMemcachedSessionLocator(
				HashAlgorithm.NATIVE_HASH);
		List<Session> sessions = new ArrayList<Session>();
		for (int i = 8080; i < 8100; i++) {
			sessions.add(new MockSession(i));
		}
		sessionLocator.updateSessions(sessions);
		for (int i = 1; i <= 10; i++) {
			String key = String.valueOf(i);

			Session session = sessionLocator.getSessionByKey(key);
			assertSame(((KetamaMemcachedSessionLocator) sessionLocator)
					.getSessionByHash(key.hashCode()), session);
			assertSame(sessionLocator.getSessionByKey(key), session);
			assertSame(sessionLocator.getSessionByKey(key), session);
		}

	}

}
