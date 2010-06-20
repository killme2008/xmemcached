package com.google.code.yanf4j.core;

/**
 * 
 * IO Event handler
 * 
 * @author boyan
 * 
 */
public interface Handler {

	public void onSessionCreated(Session session);

	public void onSessionStarted(Session session);

	public void onSessionClosed(Session session);

	public void onMessageReceived(Session session, Object msg);

	public void onMessageSent(Session session, Object msg);

	public void onExceptionCaught(Session session, Throwable throwable);

	public void onSessionExpired(Session session);

	public void onSessionIdle(Session session);

	public void onSessionConnected(Session session);

	/**
	 * �������˷����������ƣ������������Ƶ���Ϣ���ص��˷��������Ƿ������,����true������
	 * ��
	 * 
	 * @param session
	 * @param message
	 * @return
	 */
	public boolean onSessionWriteOverFlow(Session session, Object message);
}
