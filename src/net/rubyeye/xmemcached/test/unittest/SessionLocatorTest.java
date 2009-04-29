package net.rubyeye.xmemcached.test.unittest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.code.yanf4j.nio.Session;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import junit.framework.TestCase;

public class SessionLocatorTest extends TestCase {
	MemcachedSessionLocator sessionLocator;

	public void testArraySessionLocator() {
		sessionLocator = new ArrayMemcachedSessionLocator();

		List<Session> sessions = new ArrayList<Session>();
		for (int i = 8080; i < 8100; i++) {
			sessions.add(new MockSession(i));
		}
		sessionLocator.updateSessionList(sessions);
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
		sessionLocator.updateSessionList(sessions);
		assertNull(sessionLocator.getSessionByKey(key));
	}

	public void testKetamaMemcachedSessionLocator() {
		sessionLocator = new KetamaMemcachedSessionLocator(
				HashAlgorithm.NATIVE_HASH);
		List<Session> sessions = new ArrayList<Session>();
		for (int i = 8080; i < 8100; i++) {
			sessions.add(new MockSession(i));
		}
		sessionLocator.updateSessionList(sessions);
		for (int i = 1; i <= 10; i++) {
			String key = String.valueOf(i);

			assertSame(((KetamaMemcachedSessionLocator) sessionLocator)
					.getSessionByHash((long) key.hashCode()), sessionLocator
					.getSessionByKey(key));
		}

	}

}
