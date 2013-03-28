package net.rubyeye.xmemcached.test.unittest.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.test.unittest.MockSession;

import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;

public class ArrayMemcachedSessionLocatorUnitTest extends
		AbstractMemcachedSessionLocatorUnitTest {

	@Before
	public void setUp() {
		this.locator = new ArrayMemcachedSessionLocator();
	}
	
	@Test
	public void testGetSessionByKey_SessionPool() {
		MockSession session1 = new MockSession(8080);
		MockSession session2 = new MockSession(8081);
		MockSession session3 = new MockSession(8082);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session1);
		list.add(session1);
		list.add(session2);
		list.add(session2);
		list.add(session3);
	
		this.locator.updateSessions(list);

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

	}

	@Test
	public void testGetSessionByKey_MoreSessions() {
		MockSession session1 = new MockSession(8080);
		MockSession session2 = new MockSession(8081);
		MockSession session3 = new MockSession(8082);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

	}

	@Test
	public void testGetSessionByKey_MoreSessions_OneClosed() {
		MockSession session1 = new MockSession(8080);
		MockSession session2 = new MockSession(8081);
		session2.close();
		MockSession session3 = new MockSession(8082);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertSame(session3, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session3, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session3, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

	}

	@Test
	public void testGetSessionByKey_MoreSessions_OneClosed_FailureMode() {
		this.locator.setFailureMode(true);
		MockSession session1 = new MockSession(8080);
		MockSession session2 = new MockSession(8081);
		session2.close();
		MockSession session3 = new MockSession(8082);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

		assertSame(session2, this.locator.getSessionByKey("a"));
		assertSame(session3, this.locator.getSessionByKey("b"));
		assertSame(session1, this.locator.getSessionByKey("c"));

	}
}
