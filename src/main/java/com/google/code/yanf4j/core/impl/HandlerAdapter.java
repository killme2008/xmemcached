package com.google.code.yanf4j.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;

/**
 * IO Handler adapter
 * 
 * 
 * 
 * @author boyan
 * 
 */
public class HandlerAdapter implements Handler {
	private static final Logger log = LoggerFactory
			.getLogger(HandlerAdapter.class);

	public void onExceptionCaught(Session session, Throwable throwable) {

	}

	public void onMessageSent(Session session, Object message) {

	}

	public void onSessionConnected(Session session) {

	}

	public void onSessionStarted(Session session) {

	}

	public void onSessionCreated(Session session) {

	}

	public void onSessionClosed(Session session) {

	}

	public void onMessageReceived(Session session, Object message) {

	}

	public void onSessionIdle(Session session) {

	}

	public boolean onSessionWriteOverFlow(Session session, Object message) {
		log.warn("Session(" + session.getRemoteSocketAddress()
				+ ") send bytes over flow,discard message:" + message);
		return false;
	}

	public void onSessionExpired(Session session) {
		log.warn("Session(" + session.getRemoteSocketAddress()
				+ ") is expired.");
	}

}
