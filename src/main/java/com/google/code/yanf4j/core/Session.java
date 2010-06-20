package com.google.code.yanf4j.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Future;


/**
 * ���ӷ�װ
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:01:17
 */
public interface Session {

    public enum SessionStatus {
        NULL,
        READING,
        WRITING,
        IDLE,
        INITIALIZE,
        CLOSING,
        CLOSED
    }


    /**
     * Start session
     */
    public void start();


    /**
     * Async write a message to socket,return a future
     * 
     * @param packet
     * @return
     */
    public Future<Boolean> asyncWrite(Object packet);


    /**
     * Write a message,if you don't care when the message is written
     * 
     * @param packet
     */
    public void write(Object packet);


    /**
     * Check if session is closed
     * 
     * @return
     */
    public boolean isClosed();


    /**
     * Close session
     */
    public void close();


    /**
     * Return the remote end's InetSocketAddress
     * 
     * @return
     */
    public InetSocketAddress getRemoteSocketAddress();


    /**
     * ��ȡ����ip��ַ
     * 
     * @return
     */
    public InetAddress getLocalAddress();


    /**
     * Return true if using blocking write
     * 
     * @return
     */
    public boolean isUseBlockingWrite();


    /**
     * Set if using blocking write
     * 
     * @param useBlockingWrite
     */
    public void setUseBlockingWrite(boolean useBlockingWrite);


    /**
     * Return true if using blocking read
     * 
     * @return
     */
    public boolean isUseBlockingRead();


    public void setUseBlockingRead(boolean useBlockingRead);


    /**
     * Flush the write queue,this method may be no effect if OP_WRITE is
     * running.
     */
    public void flush();


    /**
     * Return true if session is expired,session is expired beacause you set the
     * sessionTimeout,if since session's last operation form now is over this
     * vlaue,isExpired return true,and Handler.onExpired() will be invoked.
     * 
     * @return
     */
    public boolean isExpired();


    /**
     * Check if session is idle
     * 
     * @return
     */
    public boolean isIdle();


    /**
     * Return current encoder
     * 
     * @return
     */
    public CodecFactory.Encoder getEncoder();


    /**
     * Set encoder
     * 
     * @param encoder
     */
    public void setEncoder(CodecFactory.Encoder encoder);


    /**
     * Return current decoder
     * 
     * @return
     */

    public CodecFactory.Decoder getDecoder();


    public void setDecoder(CodecFactory.Decoder decoder);


    /**
     * Return true if allow handling read and write concurrently,default is
     * true.
     * 
     * @return
     */
    public boolean isHandleReadWriteConcurrently();


    public void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently);


    /**
     * Return the session read buffer's byte order,big end or little end.
     * 
     * @return
     */
    public ByteOrder getReadBufferByteOrder();


    public void setReadBufferByteOrder(ByteOrder readBufferByteOrder);


    /**
     * Set a attribute attched with this session
     * 
     * @param key
     * @param value
     */
    public void setAttribute(String key, Object value);


    /**
     * Remove attribute
     * 
     * @param key
     */
    public void removeAttribute(String key);


    /**
     * Return attribute associated with key
     * 
     * @param key
     * @return
     */
    public Object getAttribute(String key);


    /**
     * Clear attributes
     */
    public void clearAttributes();


    /**
     * Return the bytes in write queue,there bytes is in memory.Use this method
     * to controll writing speed.
     * 
     * @return
     */
    public long getScheduleWritenBytes();


    /**
     * Return last operation timestamp,operation include read,write,idle
     * 
     * @return
     */
    public long getLastOperationTimeStamp();


    /**
     * return true if it is a loopback connection
     * 
     * @return
     */
    public boolean isLoopbackConnection();


    public long getSessionIdleTimeout();


    public void setSessionIdleTimeout(long sessionIdleTimeout);


    public long getSessionTimeout();


    public void setSessionTimeout(long sessionTimeout);


    public Object setAttributeIfAbsent(String key, Object value);


    public Handler getHandler();

}