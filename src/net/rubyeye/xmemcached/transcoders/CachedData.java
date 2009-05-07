// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

import java.util.Arrays;

/**
 * Cached data with its attributes.
 */
public final class CachedData {

	/**
	 * Maximum data size allowed by memcached.
	 */
	public static final int MAX_SIZE = 1024 * 1024;

	private final int flags;
	private final byte[] data;
	private final long casId;

	public long getCas() {
		return casId;
	}

	/**
	 * Get a CachedData instance for the given flags and byte array.
	 * 
	 * @param f
	 *            the flags
	 * @param d
	 *            the data
	 * @param max_size
	 *            the maximum allowable size.
	 */
	public CachedData(int f, byte[] d, int max_size, long casId) {
		super();
		if (d.length > max_size) {
			throw new IllegalArgumentException(
					"Cannot cache data larger than 1MB (you tried to cache a "
							+ d.length + " byte object)");
		}
		flags = f;
		data = d;
		this.casId = casId;
	}

	/**
	 * Get a CachedData instance for the given flags and byte array.
	 * 
	 * @param f
	 *            the flags
	 * @param d
	 *            the data
	 */
	public CachedData(int f, byte[] d) {
		this(f, d, MAX_SIZE, -1);
	}

	/**
	 * Get the stored data.
	 */
	public final byte[] getData() {
		return data;
	}

	/**
	 * Get the flags stored along with this value.
	 */
	public final int getFlags() {
		return flags;
	}

	@Override
	public String toString() {
		return "{CachedData flags=" + flags + " data=" + Arrays.toString(data)
				+ "}";
	}
}
