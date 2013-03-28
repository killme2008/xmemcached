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
package com.google.code.yanf4j.util;

/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 来自于cindy2.4的工具类，做了简化和新增
 */
import java.nio.ByteBuffer;

import com.google.code.yanf4j.config.Configuration;


public class ByteBufferUtils {
	/**
	 * 
	 * @param byteBuffer
	 * @return *
	 */
	public static final ByteBuffer increaseBufferCapatity(ByteBuffer byteBuffer) {

		if (byteBuffer == null) {
			throw new IllegalArgumentException("buffer is null");
		}
		if (Configuration.DEFAULT_INCREASE_BUFF_SIZE < 0) {
			throw new IllegalArgumentException("size less than 0");
		}

		int capacity = byteBuffer.capacity()
				+ Configuration.DEFAULT_INCREASE_BUFF_SIZE;
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity can't be negative");
		}
		ByteBuffer result = (byteBuffer.isDirect() ? ByteBuffer
				.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
		result.order(byteBuffer.order());
		byteBuffer.flip();
		result.put(byteBuffer);
		return result;
	}

	public static final void flip(ByteBuffer[] buffers) {
		if (buffers == null) {
			return;
		}
		for (ByteBuffer buffer : buffers) {
			if (buffer != null) {
				buffer.flip();
			}
		}
	}

	public static final ByteBuffer gather(ByteBuffer[] buffers) {
		if (buffers == null || buffers.length == 0) {
			return null;
		}
		ByteBuffer result = ByteBuffer.allocate(remaining(buffers));
		result.order(buffers[0].order());
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] != null) {
				result.put(buffers[i]);
			}
		}
		result.flip();
		return result;
	}

	public static final int remaining(ByteBuffer[] buffers) {
		if (buffers == null) {
			return 0;
		}
		int remaining = 0;
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] != null) {
				remaining += buffers[i].remaining();
			}
		}
		return remaining;
	}

	public static final void clear(ByteBuffer[] buffers) {
		if (buffers == null) {
			return;
		}
		for (ByteBuffer buffer : buffers) {
			if (buffer != null) {
				buffer.clear();
			}
		}
	}

	public static final String toHex(byte b) {
		return ("" + "0123456789ABCDEF".charAt(0xf & b >> 4) + "0123456789ABCDEF"
				.charAt(b & 0xf));
	}

	public static final int indexOf(ByteBuffer buffer, ByteBuffer pattern) {
		if (pattern == null || buffer == null) {
			return -1;
		}
		int n = buffer.remaining();
		int m = pattern.remaining();
		int patternPos = pattern.position();
		int bufferPos = buffer.position();
		if (n < m) {
			return -1;
		}
		for (int s = 0; s <= n - m; s++) {
			boolean match = true;
			for (int i = 0; i < m; i++) {
				if (buffer.get(s + i + bufferPos) != pattern
						.get(patternPos + i)) {
					match = false;
					break;
				}
			}
			if (match) {
				return (bufferPos + s);
			}
		}
		return -1;
	}

	public static final int indexOf(ByteBuffer buffer, ByteBuffer pattern,
			int offset) {
		if (offset < 0) {
			throw new IllegalArgumentException("offset must be greater than 0");
		}
		if (pattern == null || buffer == null) {
			return -1;
		}
		int patternPos = pattern.position();
		int n = buffer.remaining();
		int m = pattern.remaining();
		if (n < m) {
			return -1;
		}
		if (offset < buffer.position() || offset > buffer.limit()) {
			return -1;
		}
		for (int s = 0; s <= n - m; s++) {
			boolean match = true;
			for (int i = 0; i < m; i++) {
				if (buffer.get(s + i + offset) != pattern.get(patternPos + i)) {
					match = false;
					break;
				}
			}
			if (match) {
				return (offset + s);
			}
		}
		return -1;
	}

	/**
	 * 查看ByteBuffer数组是否还有剩余
	 * 
	 * @param buffers
	 *            ByteBuffers
	 * @return have remaining
	 */
	public static final boolean hasRemaining(ByteBuffer[] buffers) {
		if (buffers == null) {
			return false;
		}
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] != null && buffers[i].hasRemaining()) {
				return true;
			}
		}
		return false;
	}

	public static final int uByte(byte b) {
		return b & 0xFF;
	}
}
