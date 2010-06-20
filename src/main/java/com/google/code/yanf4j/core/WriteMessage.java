package com.google.code.yanf4j.core;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.impl.FutureImpl;




/**
 * ������Ϣ��װ
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:02:37
 */
public interface WriteMessage {

    public void writing();


    public boolean isWriting();


    public IoBuffer getWriteBuffer();


    public Object getMessage();


    public void setWriteBuffer(IoBuffer buffers);


    public FutureImpl<Boolean> getWriteFuture();

}