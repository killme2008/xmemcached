package net.rubyeye.xmemcached.test.unittest.utils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.junit.Test;

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

	@Test
	public void testGetAddressMap_IllegalArgument() {
		try {
			AddrUtil.getAddressMap(", localhost:12000,localhost:12001");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			AddrUtil.getAddressMap(" ");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testGetAddressMap_OnlyMainAddr() {
		Map<InetSocketAddress, InetSocketAddress> addressMap = AddrUtil
				.getAddressMap("localhost:12000 localhost:12001 localhost:12002 ");
		assertEquals(3, addressMap.size());
		assertNull(addressMap.get(new InetSocketAddress("localhost", 12002)));
		assertNull(addressMap.get(new InetSocketAddress("localhost", 12000)));
		assertNull(addressMap.get(new InetSocketAddress("localhost", 12001)));
	}

	@Test
	public void testGetAddressMap() {
		Map<InetSocketAddress, InetSocketAddress> addressMap = AddrUtil
				.getAddressMap("localhost:12000,localhost:12001 localhost:12002 localhost:12001,localhost:12003");
		assertEquals(3, addressMap.size());
		assertEquals(new InetSocketAddress("localhost", 12001), addressMap
				.get(new InetSocketAddress("localhost", 12000)));
		assertNull(addressMap.get(new InetSocketAddress("localhost", 12002)));
		assertEquals(addressMap.get(new InetSocketAddress("localhost", 12001)),
				new InetSocketAddress("localhost", 12003));
	}

	public void testGetAddress() {
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
}
