package net.rubyeye.xmemcached.test.unittest.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.test.unittest.MockMemcachedSession;

import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;

public class KetamaMemcachedSessionLocatorGwhalinMemcachedJavaClientUnitTest extends
		AbstractMemcachedSessionLocatorUnitTest {

	@Before
	public void setUp() {
		this.locator = new KetamaMemcachedSessionLocator(HashAlgorithm.KETAMA_HASH, false, true);
	}

	@Test
	public void testGetSessionByKey_MoreSessions() {
		MockMemcachedSession session1 = new MockMemcachedSession(8080);
		MockMemcachedSession session2 = new MockMemcachedSession(8081);
		MockMemcachedSession session3 = new MockMemcachedSession(8082);
		System.err.print(session1.getInetSocketAddressWrapper().getRemoteAddressStr());
		
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session1, this.locator.getSessionByKey("a3"));

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session1, this.locator.getSessionByKey("a3"));

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session1, this.locator.getSessionByKey("a3"));

	}

	@Test
	public void testGetSessionByKey_MoreSessions_OneClosed() {
		MockMemcachedSession session1 = new MockMemcachedSession(8080);
		MockMemcachedSession session2 = new MockMemcachedSession(8081);
		session1.close();
		MockMemcachedSession session3 = new MockMemcachedSession(8082);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session2, this.locator.getSessionByKey("a3"));

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session2, this.locator.getSessionByKey("a3"));

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session2, this.locator.getSessionByKey("a3"));

	}

	@Test
	public void testGetSessionByKey_MoreSessions_OneClosed_FailureMode() {
		this.locator.setFailureMode(true);
		MockMemcachedSession session1 = new MockMemcachedSession(8080);
		MockMemcachedSession session2 = new MockMemcachedSession(8081);
		session1.close();
		MockMemcachedSession session3 = new MockMemcachedSession(8082);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);
		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session1, this.locator.getSessionByKey("a3"));

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session1, this.locator.getSessionByKey("a3"));

		assertSame(session2, this.locator.getSessionByKey("a1"));
		assertSame(session3, this.locator.getSessionByKey("a2"));
		assertSame(session1, this.locator.getSessionByKey("a3"));
	}

}
