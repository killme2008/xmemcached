package com.google.code.yanf4j.util;

import java.util.ArrayList;
import java.util.List;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * ByteBuffer matcher based on shift-and algorithm
 * 
 * @author dennis
 * 
 */
public class ShiftAndByteBufferMatcher implements ByteBufferMatcher {

	private int[] b;
	private int mask;

	private int patternLimit;
	private int patternPos;
	private int patternLen;

	public ShiftAndByteBufferMatcher(IoBuffer pat) {
		if (pat == null || pat.remaining() == 0) {
			throw new IllegalArgumentException("blank buffer");
		}
		this.patternLimit = pat.limit();
		this.patternPos = pat.position();
		this.patternLen = pat.remaining();
		preprocess(pat);
		this.mask = 1 << (this.patternLen - 1);
	}

	/**
	 * Ԥ����
	 * 
	 * @param pat
	 */
	private void preprocess(IoBuffer pat) {
		this.b = new int[256];
		for (int i = this.patternPos; i < this.patternLimit; i++) {
			int p = ByteBufferUtils.uByte(pat.get(i));
			this.b[p] = this.b[p] | (1 << i);
		}
	}

	public final List<Integer> matchAll(IoBuffer buffer) {
		List<Integer> matches = new ArrayList<Integer>();
		int bufferLimit = buffer.limit();
		int d = 0;
		for (int pos = buffer.position(); pos < bufferLimit; pos++) {
			d <<= 1;
			d |= 1;
			d &= this.b[ByteBufferUtils.uByte(buffer.get(pos))];
			if ((d & this.mask) != 0) {
				matches.add(pos - this.patternLen + 1);
			}
		}
		return matches;
	}

	public final int matchFirst(IoBuffer buffer) {
		if (buffer == null) {
			return -1;
		}
		int bufferLimit = buffer.limit();
		int d = 0;
		for (int pos = buffer.position(); pos < bufferLimit; pos++) {
			d <<= 1;
			d |= 1;
			d &= this.b[ByteBufferUtils.uByte(buffer.get(pos))];
			if ((d & this.mask) != 0) {
				return pos - this.patternLen + 1;
			}
		}
		return -1;
	}

}
