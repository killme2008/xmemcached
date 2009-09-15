// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

/**
 * Utility class for transcoding Java types.
 */
public final class TranscoderUtils {

	private boolean packZeros;

	/**
	 * Get an instance of TranscoderUtils.
	 * 
	 * @param pack
	 *            if true, remove all zero bytes from the MSB of the packed num
	 */
	public TranscoderUtils(boolean pack) {
		super();
		this.packZeros = pack;
	}

	public final boolean isPackZeros() {
		return this.packZeros;
	}

	public final void setPackZeros(boolean packZeros) {
		this.packZeros = packZeros;
	}

	public final byte[] encodeNum(long l, int maxBytes) {
		byte[] rv = new byte[maxBytes];
		for (int i = 0; i < rv.length; i++) {
			int pos = rv.length - i - 1;
			rv[pos] = (byte) ((l >> (8 * i)) & 0xff);
		}
		if (this.packZeros) {
			int firstNon0 = 0;
			for (; firstNon0 < rv.length && rv[firstNon0] == 0; firstNon0++) {
				// Just looking for what we can reduce
			}
			if (firstNon0 > 0) {
				byte[] tmp = new byte[rv.length - firstNon0];
				System.arraycopy(rv, firstNon0, tmp, 0, rv.length - firstNon0);
				rv = tmp;
			}
		}
		return rv;
	}

	public final byte[] encodeLong(long l) {
		return encodeNum(l, 8);
	}

	public final long decodeLong(byte[] b) {
		assert b.length <= 8 : "Too long to be an long (" + b.length
				+ ") bytes";
		long rv = 0;
		for (byte i : b) {
			rv = (rv << 8) | (i < 0 ? 256 + i : i);
		}
		return rv;
	}

	public final byte[] encodeInt(int in) {
		return encodeNum(in, 4);
	}

	public final int decodeInt(byte[] in) {
		assert in.length <= 4 : "Too long to be an int (" + in.length
				+ ") bytes";
		return (int) decodeLong(in);
	}

	public final byte[] encodeByte(byte in) {
		return new byte[] { in };
	}

	public final byte decodeByte(byte[] in) {
		assert in.length <= 1 : "Too long for a byte";
		byte rv = 0;
		if (in.length == 1) {
			rv = in[0];
		}
		return rv;
	}

	public final byte[] encodeBoolean(boolean b) {
		byte[] rv = new byte[1];
		rv[0] = (byte) (b ? '1' : '0');
		return rv;
	}

	public final boolean decodeBoolean(byte[] in) {
		assert in.length == 1 : "Wrong length for a boolean";
		return in[0] == '1';
	}

}
