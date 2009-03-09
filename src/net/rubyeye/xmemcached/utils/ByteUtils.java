package net.rubyeye.xmemcached.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class ByteUtils {

	public static final byte[] CRLF = { '\r', '\n' };
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't', 's' };
	public static final byte[] DELETE = { 'd', 'e', 'l', 'e', 't', 'e' };
	public static final byte SPACE = ' ';

	public static byte[] getBytes(String k) {
		try {
			return k.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static final void setArguments(ByteBuffer bb, Object... args) {
		boolean wasFirst = true;
		for (Object o : args) {
			if (wasFirst) {
				wasFirst = false;
			} else {
				bb.put(SPACE);
			}
			if (o instanceof byte[]) {
				bb.put((byte[]) o);
			} else {
				bb.put(ByteUtils.getBytes(String.valueOf(o)));
			}
		}
		bb.put(CRLF);
	}

	public static void checkKey(String key) {
		if (key == null)
			throw new IllegalArgumentException("Key must not be null");
		byte[] keyBytes = getBytes(key);
		if (keyBytes.length > ByteUtils.MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ ByteUtils.MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for (byte b : keyBytes) {
			if (b == ' ' || b == '\n' || b == '\r' || b == 0) {
				throw new IllegalArgumentException(
						"Key contains invalid characters:  ``" + key + "''");
			}
		}
	}

	public static final int MAX_KEY_LENGTH = 250;
}
