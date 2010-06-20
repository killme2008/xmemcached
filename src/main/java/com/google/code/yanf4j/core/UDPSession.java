package com.google.code.yanf4j.core;

import java.net.SocketAddress;
import java.util.concurrent.Future;


/**
 * UDP������չ�ӿ�
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:02:22
 */
public interface UDPSession extends Session {
    /**
     * Async write message to another end
     * 
     * @param targetAddr
     * @param packet
     * @return future
     */
    public Future<Boolean> asyncWrite(SocketAddress targetAddr, Object packet);


    /**
     * Write message to another end,do not care when the message is written.
     * 
     * @param targetAddr
     * @param packet
     */
    public void write(SocketAddress targetAddr, Object packet);
}
