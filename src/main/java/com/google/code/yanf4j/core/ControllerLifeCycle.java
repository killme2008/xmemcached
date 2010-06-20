package com.google.code.yanf4j.core;

/**
 * Controller�������ڽӿ�
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����05:59:18
 */

public interface ControllerLifeCycle {

    public void notifyReady();


    public void notifyStarted();


    public void notifyAllSessionClosed();


    public void notifyException(Throwable t);


    public void notifyStopped();
}
