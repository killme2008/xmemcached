package com.google.code.yanf4j.nio;


public interface ControllerStateListener {
	public void onStarted(final Controller acceptor);

	public void onReady(final Controller acceptor);

	public void onAllSessionClosed(final Controller acceptor);

	public void onStopped(final Controller acceptor);

	public void onException(final Controller acceptor, Throwable t);
}
