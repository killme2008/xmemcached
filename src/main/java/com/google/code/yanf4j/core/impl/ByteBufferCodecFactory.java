package com.google.code.yanf4j.core.impl;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Session;



/**
 * ����빤����һ��Ĭ��ʵ�֣�ֱ�ӷ���IoBuffer
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:04:22
 */
public class ByteBufferCodecFactory implements CodecFactory {
    static final IoBuffer EMPTY_BUFFER = IoBuffer.allocate(0);

    private boolean direct;


    public ByteBufferCodecFactory() {
        this(false);
    }


    public ByteBufferCodecFactory(boolean direct) {
        super();
        this.direct = direct;
        this.encoder = new ByteBufferEncoder();
        this.decoder = new ByteBufferDecoder();
    }

    public class ByteBufferDecoder implements Decoder {

        public Object decode(IoBuffer buff, Session session) {
            if (buff == null) {
                return null;
            }
            if (buff.remaining() == 0) {
                return EMPTY_BUFFER;
            }
            byte[] bytes = new byte[buff.remaining()];
            buff.get(bytes);
            IoBuffer result = IoBuffer.allocate(bytes.length, ByteBufferCodecFactory.this.direct);
            result.put(bytes);
            result.flip();
            return result;
        }

    }

    private Decoder decoder;


    public Decoder getDecoder() {
        return this.decoder;
    }

    public class ByteBufferEncoder implements Encoder {

        public IoBuffer encode(Object message, Session session) {
            final IoBuffer msgBuffer = (IoBuffer) message;
            if (msgBuffer == null) {
                return null;
            }
            if (msgBuffer.remaining() == 0) {
                return EMPTY_BUFFER;
            }
            byte[] bytes = new byte[msgBuffer.remaining()];
            msgBuffer.get(bytes);
            IoBuffer result = IoBuffer.allocate(bytes.length, ByteBufferCodecFactory.this.direct);
            result.put(bytes);
            result.flip();
            return result;
        }

    }

    private Encoder encoder;


    public Encoder getEncoder() {
        return this.encoder;
    }

}
