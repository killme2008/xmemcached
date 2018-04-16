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

import java.io.IOException;
import java.net.InetSocketAddress;
import com.google.code.yanf4j.statistics.Statistics;

/**
 * Networking Controller
 * 
 * 
 * @author boyan
 * 
 */
public interface Controller {

  public abstract long getSessionTimeout();

  public long getSessionIdleTimeout();

  public void setSessionIdleTimeout(long sessionIdleTimeout);

  public abstract void setSessionTimeout(long sessionTimeout);

  public abstract int getSoTimeout();

  public abstract void setSoTimeout(int timeout);

  public abstract void addStateListener(ControllerStateListener listener);

  public void removeStateListener(ControllerStateListener listener);

  public abstract boolean isHandleReadWriteConcurrently();

  public abstract void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently);

  public abstract int getReadThreadCount();

  public abstract void setReadThreadCount(int readThreadCount);

  public abstract Handler getHandler();

  public abstract void setHandler(Handler handler);

  public abstract int getPort();

  public abstract void start() throws IOException;

  public abstract boolean isStarted();

  public abstract Statistics getStatistics();

  public abstract CodecFactory getCodecFactory();

  public abstract void setCodecFactory(CodecFactory codecFactory);

  public abstract void stop() throws IOException;

  public void setReceiveThroughputLimit(double receivePacketRate);

  public double getReceiveThroughputLimit();

  public double getSendThroughputLimit();

  public void setSendThroughputLimit(double sendThroughputLimit);

  public InetSocketAddress getLocalSocketAddress();

  public void setLocalSocketAddress(InetSocketAddress inetAddress);

  public int getDispatchMessageThreadCount();

  public void setDispatchMessageThreadCount(int dispatchMessageThreadPoolSize);

  public int getWriteThreadCount();

  public void setWriteThreadCount(int writeThreadCount);

  public <T> void setSocketOption(SocketOption<T> socketOption, T value);

}
