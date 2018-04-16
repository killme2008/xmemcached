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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.util.WorkerThreadFactory;

/**
 * 
 * 
 * 
 * Pool dispatcher,wrap a threadpool.
 * 
 * @author dennis
 * 
 */
public class PoolDispatcher implements Dispatcher {
  public static final int DEFAULT_POOL_QUEUE_SIZE_FACTOR = 1000;
  public static final float DEFAULT_MAX_POOL_SIZE_FACTOR = 1.25f;
  private ThreadPoolExecutor threadPool;

  public PoolDispatcher(int poolSize) {
    this(poolSize, 60, TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy(), "pool-dispatcher");
  }

  public PoolDispatcher(int poolSize, long keepAliveTime, TimeUnit unit,
      RejectedExecutionHandler rejectedExecutionHandler, String prefix) {
    this(poolSize, DEFAULT_POOL_QUEUE_SIZE_FACTOR, DEFAULT_MAX_POOL_SIZE_FACTOR, keepAliveTime,
        unit, rejectedExecutionHandler, prefix);
  }

  public PoolDispatcher(int poolSize, int poolQueueSizeFactor, float maxPoolSizeFactor,
      long keepAliveTime, TimeUnit unit, RejectedExecutionHandler rejectedExecutionHandler,
      String prefix) {
    this.threadPool = new ThreadPoolExecutor(poolSize, (int) (maxPoolSizeFactor * poolSize),
        keepAliveTime, unit, new ArrayBlockingQueue<Runnable>(poolSize * poolQueueSizeFactor),
        new WorkerThreadFactory(prefix));
    this.threadPool.setRejectedExecutionHandler(rejectedExecutionHandler);
  }

  public final void dispatch(Runnable r) {
    if (!this.threadPool.isShutdown()) {
      this.threadPool.execute(r);
    }
  }

  public void stop() {
    this.threadPool.shutdown();
    try {
      this.threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
