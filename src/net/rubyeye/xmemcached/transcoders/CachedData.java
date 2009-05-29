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

	private int flags;
	private byte[] data;
	private long cas;
	private int dataLen;

	public final int getDataLen() {
		return dataLen;
	}

	public final void setDataLen(int dataLen) {
		this.dataLen = dataLen;
	}

	public static final int getMAX_SIZE() {
		return MAX_SIZE;
	}

	public final void setFlags(int flags) {
		this.flags = flags;
	}

	public final void setData(byte[] data) {
		if (data.length > this.dataLen)
			throw new IllegalArgumentException(
					"Cannot cache data larger than 1MB (you tried to cache a "
							+ data.length + " byte object)");
		this.data = data;
	}

	public final void setCas(long cas) {
		this.cas = cas;
	}

	public long getCas() {
		return cas;
	}

	/**
	 * Get a CachedData instance for the given flags and byte array.
	 * 
	 * @param f
	 *            the flags
	 * @param d
	 *            the data
	 * @param dataLen
	 *            the maximum allowable size.
	 */
	public CachedData(int f, byte[] d, int dataLen, long casId) {
		super();
		this.dataLen = dataLen;
		if (d != null && d.length > dataLen) {
			throw new IllegalArgumentException(
					"Cannot cache data larger than 1MB (you tried to cache a "
							+ d.length + " byte object)");
		}
		flags = f;
		data = d;
		this.cas = casId;
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
