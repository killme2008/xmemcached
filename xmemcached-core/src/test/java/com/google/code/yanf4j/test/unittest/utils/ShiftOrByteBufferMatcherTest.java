package com.google.code.yanf4j.test.unittest.utils;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftOrByteBufferMatcher;

public class ShiftOrByteBufferMatcherTest  extends ByteBufferMatcherTest {
	@Override
	public ByteBufferMatcher createByteBufferMatcher(String hello) {
		ByteBufferMatcher m = new ShiftOrByteBufferMatcher(IoBuffer.wrap(hello
				.getBytes()));
		return m;
	}
}
