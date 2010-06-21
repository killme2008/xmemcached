package com.google.code.yanf4j.core;

/**
 * Dispatcher
 * 
 * @author dennis
 * 
 */
public interface Dispatcher {
	public void dispatch(Runnable r);

	public void stop();
}
