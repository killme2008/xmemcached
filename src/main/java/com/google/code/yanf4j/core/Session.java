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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

/**
 * Abstract connection
 * 
 * @author dennis
 * 
 */
public interface Session {

  public enum SessionStatus {
    NULL, READING, WRITING, IDLE, INITIALIZE, CLOSING, CLOSED
  }

  /**
   * Start session
   */
  public void start();

  /**
   * Write a message,if you don't care when the message is written
   * 
   * @param packet
   */
  public void write(Object packet);

  /**
   * Check if session is closed
   * 
   * @return
   */
  public boolean isClosed();

  /**
   * Close session
   */
  public void close();

  /**
   * Return the remote end's InetSocketAddress
   * 
   * @return
   */
  public InetSocketAddress getRemoteSocketAddress();

  public InetAddress getLocalAddress();

  /**
   * Return true if using blocking write
   * 
   * @return
   */
  public boolean isUseBlockingWrite();

  /**
   * Set if using blocking write
   * 
   * @param useBlockingWrite
   */
  public void setUseBlockingWrite(boolean useBlockingWrite);

  /**
   * Return true if using blocking read
   * 
   * @return
   */
  public boolean isUseBlockingRead();

  public void setUseBlockingRead(boolean useBlockingRead);

  /**
   * Flush the write queue,this method may be no effect if OP_WRITE is running.
   */
  public void flush();

  /**
   * Return true if session is expired,session is expired beacause you set the sessionTimeout,if
   * since session's last operation form now is over this vlaue,isExpired return true,and
   * Handler.onExpired() will be invoked.
   * 
   * @return
   */
  public boolean isExpired();

  /**
   * Check if session is idle
   * 
   * @return
   */
  public boolean isIdle();

  /**
   * Return current encoder
   * 
   * @return
   */
  public CodecFactory.Encoder getEncoder();

  /**
   * Set encoder
   * 
   * @param encoder
   */
  public void setEncoder(CodecFactory.Encoder encoder);

  /**
   * Return current decoder
   * 
   * @return
   */

  public CodecFactory.Decoder getDecoder();

  public void setDecoder(CodecFactory.Decoder decoder);

  /**
   * Return true if allow handling read and write concurrently,default is true.
   * 
   * @return
   */
  public boolean isHandleReadWriteConcurrently();

  public void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently);

  /**
   * Return the session read buffer's byte order,big end or little end.
   * 
   * @return
   */
  public ByteOrder getReadBufferByteOrder();

  public void setReadBufferByteOrder(ByteOrder readBufferByteOrder);

  /**
   * Set a attribute attched with this session
   * 
   * @param key
   * @param value
   */
  public void setAttribute(String key, Object value);

  /**
   * Remove attribute
   * 
   * @param key
   */
  public void removeAttribute(String key);

  /**
   * Return attribute associated with key
   * 
   * @param key
   * @return
   */
  public Object getAttribute(String key);

  /**
   * Clear attributes
   */
  public void clearAttributes();

  /**
   * Return the bytes in write queue,there bytes is in memory.Use this method to controll writing
   * speed.
   * 
   * @return
   */
  public long getScheduleWritenBytes();

  /**
   * Return last operation timestamp,operation include read,write,idle
   * 
   * @return
   */
  public long getLastOperationTimeStamp();

  /**
   * return true if it is a loopback connection
   * 
   * @return
   */
  public boolean isLoopbackConnection();

  public long getSessionIdleTimeout();

  public void setSessionIdleTimeout(long sessionIdleTimeout);

  public long getSessionTimeout();

  public void setSessionTimeout(long sessionTimeout);

  public Object setAttributeIfAbsent(String key, Object value);

  public Handler getHandler();

}
