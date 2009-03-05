package com.google.code.yanf4j.nio.impl;

/**
 *Copyright [2008] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.Session;

/**
 * handler 适配器类，提供默认实现
 * 
 * @author dennis
 * @param <T>
 */
public class HandlerAdapter<T> implements Handler<T> {
	private static final Log log = LogFactory.getLog(HandlerAdapter.class);

	public void onException(Session session, Throwable t) {
		log.error("session error", t);
	}

	public void onMessageSent(Session session, T t) {

	}

	public void onConnected(Session session) {

	}

	public void onSessionStarted(Session session) {

	}

	public void onSessionCreated(Session session) {

	}

	public void onSessionClosed(Session session) {

	}

	public void onReceive(Session session, T t) {

	}

	public void onIdle(Session session) {

	}

	public void onExpired(Session session) {
		log.debug("session expired " + session.getRemoteSocketAddress());
	}

}
