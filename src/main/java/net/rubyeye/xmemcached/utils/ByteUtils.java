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

import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.codec.MemcachedDecoder;
import net.rubyeye.xmemcached.monitor.Constants;

public final class ByteUtils {

	public static final String DEFAULT_CHARSET = "utf-8";
	public static final ByteBuffer SPLIT = ByteBuffer.wrap(Constants.CRLF);

	/**
	 * 防止创建
	 * 
	 */
	private ByteUtils() {
	}

	public static final byte[] getBytes(String k) {
		if (k == null || k.length() == 0) {
			throw new IllegalArgumentException("Key must not be blank");
		}
		try {
			return k.getBytes(DEFAULT_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static final void setArguments(IoBuffer bb, Object... args) {
		boolean wasFirst = true;
		for (Object o : args) {
			if (wasFirst) {
				wasFirst = false;
			} else {
				bb.put(Constants.SPACE);
			}
			if (o instanceof byte[]) {
				bb.put((byte[]) o);
			} else {
				bb.put(getBytes(String.valueOf(o)));
			}
		}
		bb.put(Constants.CRLF);
	}

	public static final void checkKey(final byte[] keyBytes) {

		if (keyBytes.length > ByteUtils.MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ ByteUtils.MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for (byte b : keyBytes) {
			if (b == ' ' || b == '\n' || b == '\r' || b == 0) {
				try {
					throw new IllegalArgumentException(
							"Key contains invalid characters:  ``"
									+ new String(keyBytes, "utf-8") + "''");

				} catch (UnsupportedEncodingException e) {
				}
			}

		}
	}

	public static final void checkKey(final String key) {
		if (key == null || key.length() == 0) {
			throw new IllegalArgumentException("Key must not be blank");
		}
		byte[] keyBytes = getBytes(key);
		if (keyBytes.length > ByteUtils.MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ ByteUtils.MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for (byte b : keyBytes) {
			if (b == ' ' || b == '\n' || b == '\r' || b == 0) {
				try {
					throw new IllegalArgumentException(
							"Key contains invalid characters:  ``"
									+ new String(keyBytes, "utf-8") + "''");

				} catch (UnsupportedEncodingException e) {
				}
			}

		}
	}

	public static final int MAX_KEY_LENGTH = 250;

	public static final int normalizeCapacity(int requestedCapacity) {
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

	public static final boolean stepBuffer(ByteBuffer buffer, int remaining) {
		if (buffer.remaining() >= remaining) {
			buffer.position(buffer.position() + remaining);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取下一行
	 * 
	 * @param buffer
	 */
	public static final String nextLine(ByteBuffer buffer) {
		/**
		 * 测试表明采用 Shift-And算法匹配 >BM算法匹配效率 > 朴素匹配 > KMP匹配，
		 * 如果你有更好的建议，请email给我(killme2008@gmail.com)
		 */
		int index = MemcachedDecoder.SPLIT_MATCHER.matchFirst(buffer);
		if (index >= 0) {
			int limit = buffer.limit();
			buffer.limit(index);
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			buffer.limit(limit);
			buffer.position(index + ByteUtils.SPLIT.remaining());
			try {
				String line = new String(bytes, "utf-8");
				return line;
			} catch (UnsupportedEncodingException e) {
				MemcachedDecoder.log.error(e, e);

			}

		}
		return null;

	}

	public static void byte2hex(byte b, StringBuffer buf) {
		char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		int high = ((b & 0xf0) >> 4);
		int low = (b & 0x0f);
		buf.append(hexChars[high]);
		buf.append(hexChars[low]);
	}

	public static void int2hex(int a, StringBuffer str) {
		str.append(Integer.toHexString(a));
	}

	public static void short2hex(int a,StringBuffer str) {
		str.append(Integer.toHexString(a));
	}
}
