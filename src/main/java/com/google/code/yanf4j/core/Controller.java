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

	public abstract void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently);

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