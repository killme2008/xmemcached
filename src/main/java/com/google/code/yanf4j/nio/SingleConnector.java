package com.google.code.yanf4j.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.Future;


/**
 * 
 * �������ӵ��������ӿ�
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ����01:12:47
 */

public interface SingleConnector {

    public Future<Boolean> connect(SocketAddress socketAddress) throws IOException;


    public Future<Boolean> send(Object msg);


    public boolean isConnected();


    public void awaitConnectUnInterrupt() throws IOException;


    public void disconnect() throws IOException;
}
