package com.google.code.yanf4j.core;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.impl.FutureImpl;

/**
 * Write message with a buffer
 * 
 * @author dennis
 * 
 */
public interface WriteMessage {

	public void writing();

	public boolean isWriting();

	public IoBuffer getWriteBuffer();

	public Object getMessage();

	public void setWriteBuffer(IoBuffer buffers);

	public FutureImpl<Boolean> getWriteFuture();

}