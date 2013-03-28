package com.google.code.yanf4j.test.unittest.utils;

import java.util.List;

import junit.framework.TestCase;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftOrByteBufferMatcher;

public abstract class ByteBufferMatcherTest extends TestCase {
	public void testMatchFirst() {
		String hello = "hel;lo";
		ByteBufferMatcher m = createByteBufferMatcher(hello);
		assertEquals(0, m.matchFirst(IoBuffer.wrap("hel;lo".getBytes())));
		assertEquals(-1, m.matchFirst(IoBuffer.wrap("hel;l0".getBytes())));
		assertEquals(6, m
				.matchFirst(IoBuffer.wrap("hello hel;lo".getBytes())));
		assertEquals(0, (m.matchFirst(IoBuffer
				.wrap("hel;lo good ".getBytes()))));
		assertEquals(7, m.matchFirst(IoBuffer.wrap("abcdefghel;lo good "
				.getBytes())));
		assertEquals(-1, m.matchFirst(IoBuffer.wrap("".getBytes())));

		assertEquals(6, m.matchFirst(IoBuffer.wrap(
				"hello hel;lo".getBytes()).position(4)));
		assertEquals(6, m.matchFirst(IoBuffer.wrap(
				"hello hel;lo".getBytes()).position(6)));
		assertEquals(-1, m.matchFirst(IoBuffer.wrap(
				"hello hel;lo".getBytes()).limit(6)));

		assertEquals(-1, m.matchFirst(null));
		assertEquals(-1, m.matchFirst(IoBuffer.allocate(0)));


		ByteBufferMatcher newline = new ShiftAndByteBufferMatcher(IoBuffer
				.wrap("\r\n".getBytes()));

		String memcachedGet = "VALUE test 0 0 100\r\nhello\r\n";
		assertEquals(memcachedGet.indexOf("\r\n"), newline
				.matchFirst(IoBuffer.wrap(memcachedGet.getBytes())));
		assertEquals(25, newline.matchFirst(IoBuffer.wrap(
				memcachedGet.getBytes()).position(20)));
	}

	public abstract ByteBufferMatcher createByteBufferMatcher(String hello);

	public void testMatchAll() {
		String memcachedGet = "VALUE test 0 0 100\r\nhello\r\n\rtestgood\r\nh\rfasdfasd\n\rdfasdfad\r\n\r\n";
		ByteBufferMatcher newline = new ShiftOrByteBufferMatcher(IoBuffer
				.wrap("\r\n".getBytes()));
		List<Integer> list = newline.matchAll(IoBuffer.wrap(memcachedGet
				.getBytes()));
		for (int i : list) {
			System.out.println(i);
		}

	}
}
