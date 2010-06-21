package com.google.code.yanf4j.util;

import java.util.List;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * ByteBuffer matcher
 * 
 * @author dennis
 * 
 */
public interface ByteBufferMatcher {

	public int matchFirst(IoBuffer buffer);

	public List<Integer> matchAll(final IoBuffer buffer);

}