package com.google.code.yanf4j.core.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;




/**
 * Handler������
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:04:48
 */
public class HandlerAdapter implements Handler {
    private static final Logger log = LoggerFactory.getLogger(HandlerAdapter.class);


    public void onExceptionCaught(Session session, Throwable throwable) {

    }


    public void onMessageSent(Session session, Object message) {

    }


    public void onMessageReject(Session session, Object msg) {
        log.error("�̳߳ط�æ����Ϣ���?�ܾ���ϢΪ" + msg);
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
        log.warn("Session(" + session.getRemoteSocketAddress() + ") send bytes over flow,discard message:" + message);
        return false;
    }


    public void onSessionExpired(Session session) {
        log.warn("Session(" + session.getRemoteSocketAddress() + ") is expired.");
    }

}
