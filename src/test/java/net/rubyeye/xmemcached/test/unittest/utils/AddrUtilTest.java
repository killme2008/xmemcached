package net.rubyeye.xmemcached.test.unittest.utils;

import java.net.InetSocketAddress;
import java.util.List;

import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.utils.AddrUtil;
import junit.framework.TestCase;

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

	public void testGetAddressesWithWeight() {
		try {
			AddrUtil.getAddressesWithWeight(null);
			fail();
		} catch (NullPointerException e) {
			assertEquals("Null host list", e.getMessage());
		}

		try {
			AddrUtil.getAddressesWithWeight("   ");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("No hosts in list:  ``" + "   " + "''", e.getMessage());
		}

		List<ReconnectRequest> addresses = AddrUtil
				.getAddressesWithWeight("localhost:12000:1 192.168.0.98:12000:2");

		assertEquals(2, addresses.size());
		assertEquals("localhost", addresses.get(0).getAddress().getHostName());
		// assertEquals("192.168.0.98",addresses.get(1).getHostName());
		assertEquals(12000, addresses.get(0).getAddress().getPort());

		assertEquals(12000, addresses.get(1).getAddress().getPort());
		assertEquals(1, addresses.get(0).getWeight());
		assertEquals(2, addresses.get(1).getWeight());
	}
}
