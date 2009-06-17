package net.rubyeye.xmemcached.utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience utilities for simplifying common address parsing.
 */
public class AddrUtil {

	/**
	 * Split a string in the form of "host:port host2:port" into a List of
	 * InetSocketAddress instances suitable for instantiating a MemcachedClient.
	 * 
	 * Note that colon-delimited IPv6 is also supported. For example: ::1:11211
	 */
	public static List<InetSocketAddress> getAddresses(String s) {
		if (s == null) {
			throw new NullPointerException("Null host list");
		}
		if (s.trim().equals("")) {
			throw new IllegalArgumentException("No hosts in list:  ``" + s
					+ "''");
		}
		ArrayList<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();

		for (String hoststuff : s.split(" ")) {
			int finalColon = hoststuff.lastIndexOf(':');
			if (finalColon < 1) {
				throw new IllegalArgumentException("Invalid server ``"
						+ hoststuff + "'' in list:  " + s);

			}
			String hostPart = hoststuff.substring(0, finalColon);
			String portNum = hoststuff.substring(finalColon + 1);

			addrs
					.add(new InetSocketAddress(hostPart, Integer
							.parseInt(portNum)));
		}
		assert !addrs.isEmpty() : "No addrs found";
		return addrs;
	}

	/**
	 * Get one InetSocketAddress.This method is deprecated,replace with
	 * getOneAddress
	 * 
	 * @param server
	 * @return
	 */
	@Deprecated
	public static InetSocketAddress getAddress(String server) {
		return getOneAddress(server);
	}

	public static InetSocketAddress getOneAddress(String server) {
		if (server == null) {
			throw new NullPointerException("Null host");
		}
		if (server.trim().equals("")) {
			throw new IllegalArgumentException("No hosts in:  ``" + server
					+ "''");
		}

		int finalColon = server.lastIndexOf(':');
		if (finalColon < 1) {
			throw new IllegalArgumentException("Invalid server ``" + server
					+ "'' in list:  " + server);

		}
		String hostPart = server.substring(0, finalColon);
		String portNum = server.substring(finalColon + 1);
		return new InetSocketAddress(hostPart, Integer.parseInt(portNum));
	}
}
