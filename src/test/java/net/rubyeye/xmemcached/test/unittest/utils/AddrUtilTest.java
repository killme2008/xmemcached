package net.rubyeye.xmemcached.test.unittest.utils;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class AddrUtilTest extends TestCase {
	public void testGetAddresses() {
		try {
			AddrUtil.getAddresses(null);
			fail();
		} catch (NullPointerException e) {
			assertEquals("Null host list", e.getMessage());
		}

		try {
			AddrUtil.getAddresses("   ");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("No hosts in list:  ``" + "   " + "''", e.getMessage());
		}

		List<InetSocketAddress> addresses = AddrUtil
				.getAddresses("localhost:12000 192.168.0.98:12000");

		assertEquals(2, addresses.size());
		assertEquals("localhost", addresses.get(0).getHostName());
		// assertEquals("192.168.0.98",addresses.get(1).getHostName());
		assertEquals(12000, addresses.get(0).getPort());
		assertEquals(12000, addresses.get(1).getPort());
	}
}
