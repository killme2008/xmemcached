/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.rubyeye.xmemcached.buffer.ByteBufferWrapper;

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

	public static final void setArguments(ByteBufferWrapper bb, Object... args) {
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
		if (key == null || key.trim().length() == 0)
			throw new IllegalArgumentException("Key must not be blank");
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

	public static int normalizeCapacity(int requestedCapacity) {
		switch (requestedCapacity) {
		case 0:
		case 1 << 0:
		case 1 << 1:
		case 1 << 2:
		case 1 << 3:
		case 1 << 4:
		case 1 << 5:
		case 1 << 6:
		case 1 << 7:
		case 1 << 8:
		case 1 << 9:
		case 1 << 10:
		case 1 << 11:
		case 1 << 12:
		case 1 << 13:
		case 1 << 14:
		case 1 << 15:
		case 1 << 16:
		case 1 << 17:
		case 1 << 18:
		case 1 << 19:
		case 1 << 21:
		case 1 << 22:
		case 1 << 23:
		case 1 << 24:
		case 1 << 25:
		case 1 << 26:
		case 1 << 27:
		case 1 << 28:
		case 1 << 29:
		case 1 << 30:
		case Integer.MAX_VALUE:
			return requestedCapacity;
		}

		int newCapacity = 1;
		while (newCapacity < requestedCapacity) {
			newCapacity <<= 1;
			if (newCapacity < 0) {
				return Integer.MAX_VALUE;
			}
		}
		return newCapacity;
	}
}
