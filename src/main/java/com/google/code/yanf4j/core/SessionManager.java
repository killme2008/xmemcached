package com.google.code.yanf4j.core;

/**
 * Session manager
 * 
 * @author dennis
 * 
 */
public interface SessionManager {
	/**
	 * Register session to controller
	 * 
	 * @param session
	 */
	public void registerSession(Session session);

	/**
	 * Unregister session
	 * 
	 * @param session
	 */
	public void unregisterSession(Session session);
}
