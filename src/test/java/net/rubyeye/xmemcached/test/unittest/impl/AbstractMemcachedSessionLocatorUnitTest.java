package net.rubyeye.xmemcached.test.unittest.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.test.unittest.MockSession;

import org.junit.Test;

import com.google.code.yanf4j.core.Session;

public abstract class AbstractMemcachedSessionLocatorUnitTest {
	protected MemcachedSessionLocator locator;

	@Test
	public void testGetSessionByKey_EmptyList() {
		assertNull(this.locator.getSessionByKey("test"));
	}

	@Test
	public void testGetSessionByKey_OneSession() {
		MockSession session = new MockSession(8080);
		List<Session> list = new ArrayList<Session>();
		list.add(session);
		this.locator.updateSessions(list);

		assertSame(session, this.locator.getSessionByKey("a"));
		assertSame(session, this.locator.getSessionByKey("b"));
		assertSame(session, this.locator.getSessionByKey("c"));

	}
}
