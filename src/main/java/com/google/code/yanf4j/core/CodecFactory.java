package com.google.code.yanf4j.core;

import com.google.code.yanf4j.buffer.IoBuffer;




/**
 * ����빤����
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����05:57:19
 */
public interface CodecFactory {

    public interface Encoder {
        public IoBuffer encode(Object message, Session session);
    }

    public interface Decoder {
        public Object decode(IoBuffer buff, Session session);
    }


    public Encoder getEncoder();


    public Decoder getDecoder();
}
