package com.google.code.yanf4j.core;

/**
 * �߳��ɷ���
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:00:01
 */
public interface Dispatcher {
    public void dispatch(Runnable r);


    public void stop();
}
