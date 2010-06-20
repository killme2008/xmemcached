package com.google.code.yanf4j.core.impl;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.WriteMessage;




/**
 * ������Ϣ��װʵ��
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:06:01
 */
public class WriteMessageImpl implements WriteMessage {

    protected Object message;

    protected IoBuffer buffer;

    protected FutureImpl<Boolean> writeFuture;

    protected volatile boolean writing;


    public final void writing() {
        this.writing = true;
    }


    public final boolean isWriting() {
        return this.writing;
    }


    public WriteMessageImpl(Object message, FutureImpl<Boolean> writeFuture) {
        this.message = message;
        this.writeFuture = writeFuture;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.google.code.yanf4j.nio.IWriteMessage#getBuffers()
     */
    public synchronized final IoBuffer getWriteBuffer() {
        return this.buffer;
    }


    public synchronized final void setWriteBuffer(IoBuffer buffers) {
        this.buffer = buffers;

    }


    public final FutureImpl<Boolean> getWriteFuture() {
        return this.writeFuture;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.google.code.yanf4j.nio.IWriteMessage#getMessage()
     */
    public final Object getMessage() {
        return this.message;
    }
}