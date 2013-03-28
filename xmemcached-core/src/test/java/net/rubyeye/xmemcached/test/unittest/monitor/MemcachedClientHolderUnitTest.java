package net.rubyeye.xmemcached.test.unittest.monitor;

import net.rubyeye.xmemcached.monitor.MemcachedClientNameHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemcachedClientHolderUnitTest {

	@Before
	@After
	public void setUpTearDown() {
		MemcachedClientNameHolder.clear();

	}

	@Test
	public void setGetClearName() {
		assertNull(MemcachedClientNameHolder.getName());

		MemcachedClientNameHolder.setName("MemcachedClient-1");

		assertEquals("MemcachedClient-1", MemcachedClientNameHolder.getName());

		MemcachedClientNameHolder.clear();
		assertNull(MemcachedClientNameHolder.getName());

	}

	@Test
	public void setTwice() {
		assertNull(MemcachedClientNameHolder.getName());

		MemcachedClientNameHolder.setName("MemcachedClient-1");

		assertEquals("MemcachedClient-1", MemcachedClientNameHolder.getName());
		MemcachedClientNameHolder.setName("MemcachedClient-2");
		assertEquals("MemcachedClient-2", MemcachedClientNameHolder.getName());
		MemcachedClientNameHolder.clear();
		assertNull(MemcachedClientNameHolder.getName());
	}

}
