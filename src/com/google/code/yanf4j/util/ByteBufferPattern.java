package com.google.code.yanf4j.util;

import java.nio.ByteBuffer;


public class ByteBufferPattern {
	private int[] next;

	private int patLen;

	private ByteBuffer patternBuffer;

	private ByteBufferPattern(ByteBuffer patternBuffer) {
		this.patternBuffer = patternBuffer;
		this.next = ByteBufferUtils.compile(patternBuffer);
		this.patLen = this.patternBuffer.limit();
	}

	public int[] getNext() {
		return this.next;
	}

	public int getPatLimit() {
		return patLen;
	}

	public ByteBuffer getPatternBuffer() {
		return patternBuffer;
	}

	public static ByteBufferPattern compile(ByteBuffer pattern) {
		if (pattern == null)
			return null;
		return new ByteBufferPattern(pattern);
	}
}
