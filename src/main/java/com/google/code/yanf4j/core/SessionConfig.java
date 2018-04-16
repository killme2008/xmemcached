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
package com.google.code.yanf4j.core;

import java.util.Queue;
import com.google.code.yanf4j.statistics.Statistics;

/**
 * Session configuration
 * 
 * @author dennis
 * 
 */
public class SessionConfig {
  public final Handler handler;
  public final CodecFactory codecFactory;
  public final Statistics statistics;
  public final Queue<WriteMessage> queue;
  public final Dispatcher dispatchMessageDispatcher;
  public final boolean handleReadWriteConcurrently;
  public final long sessionTimeout;
  public final long sessionIdelTimeout;

  public SessionConfig(Handler handler, CodecFactory codecFactory, Statistics statistics,
      Queue<WriteMessage> queue, Dispatcher dispatchMessageDispatcher,
      boolean handleReadWriteConcurrently, long sessionTimeout, long sessionIdelTimeout) {

    this.handler = handler;
    this.codecFactory = codecFactory;
    this.statistics = statistics;
    this.queue = queue;
    this.dispatchMessageDispatcher = dispatchMessageDispatcher;
    this.handleReadWriteConcurrently = handleReadWriteConcurrently;
    this.sessionTimeout = sessionTimeout;
    this.sessionIdelTimeout = sessionIdelTimeout;
  }
}
