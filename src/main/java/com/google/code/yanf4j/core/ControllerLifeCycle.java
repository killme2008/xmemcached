package com.google.code.yanf4j.core;

/**
 * Controller lifecycle mark interface
 * 
 * @author boyan
 * 
 */

public interface ControllerLifeCycle {

	public void notifyReady();

	public void notifyStarted();

	public void notifyAllSessionClosed();

	public void notifyException(Throwable t);

	public void notifyStopped();
}
