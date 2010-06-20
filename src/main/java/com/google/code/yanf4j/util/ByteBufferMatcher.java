package com.google.code.yanf4j.util;

import java.util.List;

import com.google.code.yanf4j.buffer.IoBuffer;




/**
 * ByteBufferƥ�����ӿ�
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:19:40
 */
public interface ByteBufferMatcher {

    public int matchFirst(IoBuffer buffer);


    public List<Integer> matchAll(final IoBuffer buffer);

}