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
package com.google.code.yanf4j.nio;

/**
 * 协议处理handler
 * 
 * @author dennis(killme2008@gmail.com)
 * @param <T>
 */
public interface Handler<T> {

	public void onSessionCreated(Session session);

	public void onSessionStarted(Session session);

	public void onSessionClosed(Session session);

	public void onReceive(Session session, T t);

	public void onMessageSent(Session session, T t);

	public void onException(Session session, Throwable t);

	public void onExpired(Session session);

	public void onIdle(Session session);
	
	public void onConnected(Session session);

}
