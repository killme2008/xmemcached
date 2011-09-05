package net.rubyeye.xmemcached.test.unittest.impl;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.RoundRobinMemcachedSessionLocator;
import net.rubyeye.xmemcached.test.unittest.MockSession;

import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;

public class RoundRobinMemcachedSessionLocatorUnitTest extends
		AbstractMemcachedSessionLocatorUnitTest {
	@Before
	public void setUp() {
		this.locator = new RoundRobinMemcachedSessionLocator();
	}

	@Test
	public void testGetSessionByKey() {
		MockSession session1 = new MockSession(8080);
		MockSession session2 = new MockSession(8080);
		MockSession session3 = new MockSession(8080);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertSame(session1, this.locator.getSessionByKey("a"));
		assertSame(session2, this.locator.getSessionByKey("b"));
		assertSame(session3, this.locator.getSessionByKey("c"));

		assertSame(session1, this.locator.getSessionByKey("a"));
		assertSame(session2, this.locator.getSessionByKey("b"));
		assertSame(session3, this.locator.getSessionByKey("c"));
	}

}
