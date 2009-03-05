package com.google.code.yanf4j.nio;

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
import java.io.IOException;
import java.net.InetSocketAddress;
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.nio.impl.Reactor;

/**
 * yanf4j框架主接口
 * 
 * @author dennis
 * 
 */
public interface Controller {

	public abstract long getSessionTimeout();

	public abstract void setSessionTimeout(long sessionTimeout);

	public abstract int getSoTimeout();

	public abstract void setSoTimeout(int timeout);

	public abstract void addStateListener(ControllerStateListener listener);

	public abstract boolean isHandleReadWriteConcurrently();

	public abstract void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently);

	public abstract int getReadThreadCount();

	public abstract void setReadThreadCount(int readThreadCount);

	public abstract int getReceiveBufferSize();

	public abstract void setReceiveBufferSize(int receiveBufferSize);

	@SuppressWarnings("unchecked")
	public abstract Handler getHandler();

	@SuppressWarnings("unchecked")
	public abstract void setHandler(Handler handler);

	public abstract int getPort();

	public abstract void setPort(int port);

	public abstract boolean isReuseAddress();

	public abstract void setReuseAddress(boolean reuseAddress);

	public abstract void start() throws IOException;

	public abstract boolean isStarted();

	public abstract Statistics getStatistics();

	@SuppressWarnings("unchecked")
	public abstract CodecFactory getCodecFactory();

	@SuppressWarnings("unchecked")
	public abstract void setCodecFactory(CodecFactory codecFactory);

	@SuppressWarnings("unchecked")
	public abstract void open(Configuration configuration, Handler handler)
			throws IOException;

	@SuppressWarnings("unchecked")
	public abstract void open(int port, boolean reuseAddr, Handler handler)
			throws IOException;

	@SuppressWarnings("unchecked")
	public abstract void open(int port, boolean reuseAddr, Handler handler,
			CodecFactory codecFactory) throws IOException;

	@SuppressWarnings("unchecked")
	public abstract void open(int port, Handler handler) throws IOException;

	@SuppressWarnings("unchecked")
	public abstract void open(int port, Handler handler,
			CodecFactory codecFactory) throws IOException;

	@SuppressWarnings("unchecked")
	public void open(Configuration configuration, Handler handler,
			CodecFactory codecFactory) throws IOException;

	public abstract void stop();

	public abstract double getReceivePacketRate();

	public abstract void setReceivePacketRate(double recvRate);

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, boolean reuseAddr,
			Handler handler, CodecFactory codecFactory) throws IOException;

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, boolean reuseAddr,
			Handler handler) throws IOException;

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, Handler handler)
			throws IOException;

	@SuppressWarnings("unchecked")
	public void open(InetSocketAddress inetSocketAddress, Handler handler,
			CodecFactory codecFactory) throws IOException;

	public InetSocketAddress getLocalSocketAddress();

	public void setLocalSocketAddress(InetSocketAddress inetAddress);

	public void wakeup();

	public void wakeup(Session session, EventType eventType);

	/**
	 * 设置session发送缓冲队列的高水位标记，字节为单位
	 * 当队列中的总字节数超过这个数值时，Session的send方法将阻塞，直到队列中的总字节数下降到低水位标记以下。
	 * 
	 * @param highWaterMark
	 */
	public void setSessionWriteQueueHighWaterMark(int highWaterMark);

	/**
	 * 设置session发送缓冲队列的低水位标记，字节为单位 直到队列中的总字节数低于这个数值时，Session的send方法将解除阻塞
	 * 
	 * @param highWaterMark
	 */
	public void setSessionWriteQueueLowWaterMark(int lowWaterMark);

	public Reactor getReactor();
}