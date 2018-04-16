/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
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
  private static final Logger log = LoggerFactory.getLogger(HandlerAdapter.class);

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

  public void onSessionExpired(Session session) {
    log.warn("Session(" + session.getRemoteSocketAddress() + ") is expired.");
  }

}
