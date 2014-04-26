// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Cached data with its attributes.
 */
public final class CachedData {

	/**
	 * Maximum data size allowed by memcached.
	 */
	public static int MAX_SIZE = 1024 * 1024;

	protected int flag;
	protected long cas;
	private int capacity = -1;

	protected int size = 0;
	
	//cache decoded object.
	public volatile Object decodedObject;
	
	//padding fields
	public long p1,p2,p3,p4;
	public int p5;
	
	protected byte[] data;

	public final int getSize() {
		return this.size;
	}

	public final void fillData(ByteBuffer buffer, int offset, int length) {
		buffer.get(this.data, offset, length);
		this.size += length;
	}

	public final void fillData(ByteBuffer buffer, int length) {
		buffer.get(this.data, this.size, length);
		this.size += length;
	}

	public final int getCapacity() {
		return this.capacity;
	}

	public final void setSize(int size) {
		this.size = size;
	}

	public final void setCapacity(int dataLen) {
		this.capacity = dataLen;
	}

	public static final int getMAX_SIZE() {
		return MAX_SIZE;
	}

	public final void setFlag(int flags) {
		this.flag = flags;
	}

	public final void setData(byte[] data) {
		if (data.length > this.capacity) {
			throw new IllegalArgumentException(
					"Cannot cache data larger than 1MB (you tried to cache a "
							+ data.length + " byte object)");
		}
		this.data = data;
	}

	public final void setCas(long cas) {
		this.cas = cas;
	}

	public long getCas() {
		return this.cas;
	}

	public CachedData() {
		super();
	}

	/**
	 * Get a CachedData instance for the given flags and byte array.
	 * 
	 * @param f
	 *            the flags
	 * @param d
	 *            the data
	 * @param capacity
	 *            the maximum allowable size.
	 */
	public CachedData(int f, byte[] d, int capacity, long casId) {
		super();
		this.capacity = capacity;
		this.size = d != null ? d.length : 0;
		if (d != null && d.length > capacity) {
			throw new IllegalArgumentException(
					"Cannot cache data larger than 1MB (you tried to cache a "
							+ d.length + " byte object)");
		}
		this.flag = f;
		this.data = d;
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
		return this.data;
	}

	/**
	 * Get the flags stored along with this value.
	 */
	public final int getFlag() {
		return this.flag;
	}

	@Override
	public String toString() {
		return "{CachedData flags=" + this.flag + " data="
				+ Arrays.toString(this.data) + "}";
	}

	public int remainingCapacity() {
		if (getCapacity() < 0) {
			return -1;
		}
		int remainingCapacity = getCapacity() - getSize();
		return remainingCapacity;
	}
}
