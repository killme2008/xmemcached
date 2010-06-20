package com.google.code.yanf4j.nio.output;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.nio.impl.NioTCPSession;




/**
 * ��ʽAPIд
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:18:42
 */
public class ChannelOutputStream extends OutputStream {
    private final ByteBuffer writeBuffer;
    private final NioTCPSession session;


    public ChannelOutputStream(NioTCPSession session, int capacity, boolean direct) {
        if (direct) {
            this.writeBuffer = ByteBuffer.allocateDirect(capacity <= 0 ? 1024 : capacity);
        }
        else {
            this.writeBuffer = ByteBuffer.allocate(capacity <= 0 ? 1024 : capacity);
        }
        this.session = session;
    }


    @Override
    public void write(int b) throws IOException {
        this.writeBuffer.put((byte) b);

    }


    @Override
    public void flush() throws IOException {
        this.writeBuffer.flip();
        this.session.write(IoBuffer.wrap(this.writeBuffer));
    }


    public Future<Boolean> asyncFlush() {
        this.writeBuffer.flip();
        return this.session.asyncWrite(IoBuffer.wrap(this.writeBuffer));
    }


    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Please use Session.close() to close iostream.");
    }
}
