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

	public void testOneAddress() {
		try {
			AddrUtil.getOneAddress(null);
			fail();
		} catch (NullPointerException e) {
			assertEquals("Null host", e.getMessage());
		}

		try {
			AddrUtil.getOneAddress("   ");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("No hosts in:  ``" + "   " + "''", e.getMessage());
		}
		
		try {
			AddrUtil.getOneAddress("localhost");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid server ``localhost''", e.getMessage());
		}

		InetSocketAddress addresses = AddrUtil.getOneAddress("localhost:12000");

		assertEquals("localhost", addresses.getHostName());
		// assertEquals("192.168.0.98",addresses.get(1).getHostName());
		assertEquals(12000, addresses.getPort());
	}
	
	public void testGetAddress() {
		try {
			AddrUtil.getAddress(null);
			fail();
		} catch (NullPointerException e) {
			assertEquals("Null host", e.getMessage());
		}

		try {
			AddrUtil.getAddress("   ");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("No hosts in:  ``" + "   " + "''", e.getMessage());
		}
		
		try {
			AddrUtil.getAddress("localhost");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid server ``localhost''", e.getMessage());
		}


		InetSocketAddress addresses = AddrUtil.getAddress("localhost:12000");

		assertEquals("localhost", addresses.getHostName());
		// assertEquals("192.168.0.98",addresses.get(1).getHostName());
		assertEquals(12000, addresses.getPort());
	}
}
