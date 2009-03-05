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
	public static ByteBuffer increaseBufferCapatity(ByteBuffer byteBuffer) {

		if (byteBuffer == null)
			throw new IllegalArgumentException("buffer is null");
		if (Configuration.DEFAULT_INCREASE_BUFF_SIZE < 0)
			throw new IllegalArgumentException("size less than 0");

		int capacity = byteBuffer.capacity()
				+ Configuration.DEFAULT_INCREASE_BUFF_SIZE;
		if (capacity < 0)
			throw new IllegalArgumentException("capacity can't be negative");
		ByteBuffer result = (byteBuffer.isDirect() ? ByteBuffer
				.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
		result.order(byteBuffer.order());
		byteBuffer.flip();
		result.put(byteBuffer);
		return result;
	}

	public static void flip(ByteBuffer[] buffers) {
		if (buffers == null)
			return;
		for (ByteBuffer buffer : buffers) {
			if (buffer != null)
				buffer.flip();
		}
	}

	public static ByteBuffer gather(ByteBuffer[] buffers) {
		if (buffers == null || buffers.length == 0)
			return null;
		ByteBuffer result = ByteBuffer.allocate(remaining(buffers));
		result.order(buffers[0].order());
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] != null)
				result.put(buffers[i]);
		}
		result.flip();
		return result;
	}

	public static int remaining(ByteBuffer[] buffers) {
		if (buffers == null)
			return 0;
		int remaining = 0;
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] != null)
				remaining += buffers[i].remaining();
		}
		return remaining;
	}

	public static void clear(ByteBuffer[] buffers) {
		if (buffers == null)
			return;
		for (ByteBuffer buffer : buffers) {
			if (buffer != null)
				buffer.clear();
		}
	}

	public static String toHex(byte b) {
		return ("" + "0123456789ABCDEF".charAt(0xf & b >> 4) + "0123456789ABCDEF"
				.charAt(b & 0xf));
	}

	public static int kmpIndexOf(ByteBuffer buffer, ByteBuffer pattern) {
		if (pattern == null || buffer == null)
			return -1;
		return kmpIndexOf(buffer, pattern, buffer.position());
	}

	public static int[] compile(ByteBuffer pattern) {
		if(pattern==null||pattern.remaining()==0)
			return new int[0];
		int patLen = pattern.remaining();
		int[] next = new int[patLen];
		int i = 0, j = -1;
		next[0] = -1;
		while (i < patLen - 1) {
			if (j == -1 || pattern.get(i) == pattern.get(j)) {
				i++;
				j++;
				if (pattern.get(i) != pattern.get(j))
					next[i] = j;
				else
					next[i] = next[j];
			} else
				j = next[j];
		}
		return next;
	}

	public static int kmpIndexOf(ByteBuffer buffer, ByteBufferPattern pattern,
			int offset) {
		if (pattern == null)
			return -1;
		int[] next = pattern.getNext();
		int buffLimit = buffer.limit();
		return match(buffer, pattern.getPatternBuffer(), offset, pattern
				.getPatLimit(), buffLimit, next);
	}

	public static int kmpIndexOf(ByteBuffer buffer, ByteBufferPattern pattern) {
		if (pattern == null||buffer==null)
			return -1;
		int[] next = pattern.getNext();
		int buffLimit = buffer.limit();
		return match(buffer, pattern.getPatternBuffer(), buffer.position(),
				pattern.getPatLimit(), buffLimit, next);
	}

	public static int kmpIndexOf(ByteBuffer buffer, ByteBuffer pattern,
			int offset) {
		if (pattern == null || buffer == null)
			return -1;
		if (offset > buffer.limit())
			return -1;
		if (pattern.remaining() == 0)
			return 0;
		int patLimit = pattern.limit();
		int buffLimit = buffer.limit();
		int[] next = compile(pattern);
		return match(buffer, pattern, offset, patLimit, buffLimit, next);
	}

	private static int match(ByteBuffer buffer, ByteBuffer pattern, int offset,
			int patLimit, int buffLimit, int[] next) {
		int i, j;
		i = offset;
		j = pattern.position();
		while (i < buffLimit && j < patLimit) {
			if (j == -1 || pattern.get(j) == buffer.get(i)) {
				i++;
				j++;
			} else
				j = next[j];
		}
		if (j == patLimit)
			return i - j; // match found at offset i - j
		else
			return -1; // not found
	}

	public static int indexOf(ByteBuffer buffer, ByteBuffer pattern) {
		if (pattern == null || buffer == null)
			return -1;
		int patternPos = pattern.position();
		int patternLen = pattern.remaining();
		int lastIndex = buffer.limit() - patternLen + 1;

		Label: for (int i = buffer.position(); i < lastIndex; i++) {
			for (int j = 0; j < patternLen; j++) {
				if (buffer.get(i + j) != pattern.get(patternPos + j))
					continue Label;
			}
			return i;
		}
		return -1;
	}

	public static int indexOf(ByteBuffer buffer, ByteBuffer pattern, int offset) {
		if (pattern == null || buffer == null)
			return -1;
		int patternPos = pattern.position();
		int patternLen = pattern.remaining();
		int lastIndex = buffer.limit() - patternLen + 1;
		if (offset < buffer.position() || offset >= lastIndex)
			return -1;
		Label: for (int i = offset; i < lastIndex; i++) {
			for (int j = 0; j < patternLen; j++) {
				if (buffer.get(i + j) != pattern.get(patternPos + j))
					continue Label;
			}
			return i;
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
	public static boolean hasRemaining(ByteBuffer[] buffers) {
		if (buffers == null)
			return false;
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] != null && buffers[i].hasRemaining())
				return true;
		}
		return false;
	}
}
