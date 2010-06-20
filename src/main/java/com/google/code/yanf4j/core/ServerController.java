package com.google.code.yanf4j.core;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * ���������ƽӿ�
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:00:59
 */
public interface ServerController extends Controller {

    public void bind(InetSocketAddress localAddress) throws IOException;


    public void bind(int port) throws IOException;


    public void unbind() throws IOException;

}
