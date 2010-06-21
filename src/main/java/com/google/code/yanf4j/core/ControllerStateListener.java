package com.google.code.yanf4j.core;

/**
 * 
 * Controller state listener
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����05:59:44
 */
public interface ControllerStateListener {

	/**
	 * When controller is started
	 * 
	 * @param controller
	 */
	public void onStarted(final Controller controller);

	/**
	 * When controller is ready
	 * 
	 * @param controller
	 */
	public void onReady(final Controller controller);

	/**
	 * When all connections are closed
	 * 
	 * @param controller
	 */
	public void onAllSessionClosed(final Controller controller);

	/**
	 * When controller has been stopped
	 * 
	 * @param controller
	 */
	public void onStopped(final Controller controller);

	public void onException(final Controller controller, Throwable t);
}
