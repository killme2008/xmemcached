package com.google.code.yanf4j.core;

/**
 * ҵ�������ӿ�
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:00:24
 */
public interface Handler {

    public void onSessionCreated(Session session);


    public void onSessionStarted(Session session);


    public void onSessionClosed(Session session);


    public void onMessageReceived(Session session, Object msg);


    /**
     * ��������dispatchMessageThreadCount>0������£��̳߳ط�æ��ʱ����Ϣ�����ܾ�ص��˷���
     * @param session
     * @param msg
     */
    public void onMessageReject(Session session, Object msg);


    public void onMessageSent(Session session, Object msg);


    public void onExceptionCaught(Session session, Throwable throwable);


    public void onSessionExpired(Session session);


    public void onSessionIdle(Session session);


    public void onSessionConnected(Session session);


    /**
     * �������˷����������ƣ������������Ƶ���Ϣ���ص��˷��������Ƿ������,����true��������
     * @param session
     * @param message
     * @return
     */
    public boolean onSessionWriteOverFlow(Session session, Object message);
}
