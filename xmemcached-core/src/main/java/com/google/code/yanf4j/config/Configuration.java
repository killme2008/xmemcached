/**
 *Copyright [2009-2010] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.config;

/**
 * Networking configuration
 * 
 * @author dennis
 * 
 */
public class Configuration {

	/**
	 * Read buffer size per connection
	 */
	private int sessionReadBufferSize = 32 * 1024;

	/**
	 * Socket SO_TIMEOUT option
	 */
	private int soTimeout = 0;

	/**
	 * Thread count for processing WRITABLE event
	 */
	private int writeThreadCount = 0;

	/**
	 * Whether to enable statistics
	 */
	private boolean statisticsServer = false;

	/**
	 * Whether to handle read write concurrently,default is true
	 */
	private boolean handleReadWriteConcurrently = true;

	/**
	 * Thread coount for processing message dispatching
	 */
	private int dispatchMessageThreadCount = 0;

	/**
	 * THread count for processing READABLE event
	 */
	private int readThreadCount = 1;

	/**
	 * Increasing buffer size per time
	 */
	public static final int DEFAULT_INCREASE_BUFF_SIZE = 32 * 1024;

	/**
	 * Max read buffer size for connection
	 */
	public static int MAX_READ_BUFFER_SIZE = 128 * 1024;

	/**
	 * check session idle interval
	 */
	private volatile long checkSessionTimeoutInterval = 1000L;

	public final int getWriteThreadCount() {
		return this.writeThreadCount;
	}

	public final int getDispatchMessageThreadCount() {
		return this.dispatchMessageThreadCount;
	}

	public final void setDispatchMessageThreadCount(
			int dispatchMessageThreadCount) {
		this.dispatchMessageThreadCount = dispatchMessageThreadCount;
	}

	public final void setWriteThreadCount(int writeThreadCount) {
		this.writeThreadCount = writeThreadCount;
	}

	private volatile long sessionIdleTimeout = 5000L;

	/**
	 * @see setSessionIdleTimeout
	 * @return
	 */
	public final long getSessionIdleTimeout() {
		return this.sessionIdleTimeout;
	}

	public final void setSessionIdleTimeout(long sessionIdleTimeout) {
		this.sessionIdleTimeout = sessionIdleTimeout;
	}

	/**
	 * @see setSessionReadBufferSize
	 * @return
	 */
	public final int getSessionReadBufferSize() {
		return this.sessionReadBufferSize;
	}

	public final boolean isHandleReadWriteConcurrently() {
		return this.handleReadWriteConcurrently;
	}

	public final int getSoTimeout() {
		return this.soTimeout;
	}

	protected long statisticsInterval = 5 * 60 * 1000L;

	public final long getStatisticsInterval() {
		return this.statisticsInterval;
	}

	public final void setStatisticsInterval(long statisticsInterval) {
		this.statisticsInterval = statisticsInterval;
	}

	public final void setSoTimeout(int soTimeout) {
		if (soTimeout < 0) {
			throw new IllegalArgumentException("soTimeout<0");
		}
		this.soTimeout = soTimeout;
	}

	public final void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	public final void setSessionReadBufferSize(int tcpHandlerReadBufferSize) {
		if (tcpHandlerReadBufferSize <= 0) {
			throw new IllegalArgumentException("tcpHandlerReadBufferSize<=0");
		}
		this.sessionReadBufferSize = tcpHandlerReadBufferSize;
	}

	public final boolean isStatisticsServer() {
		return this.statisticsServer;
	}

	public final void setStatisticsServer(boolean statisticsServer) {
		this.statisticsServer = statisticsServer;
	}

	/**
	 * @see setReadThreadCount
	 * @return
	 */
	public final int getReadThreadCount() {
		return this.readThreadCount;
	}

	public final void setReadThreadCount(int readThreadCount) {
		if (readThreadCount < 0) {
			throw new IllegalArgumentException("readThreadCount<0");
		}
		this.readThreadCount = readThreadCount;
	}

	public void setCheckSessionTimeoutInterval(long checkSessionTimeoutInterval) {
		this.checkSessionTimeoutInterval = checkSessionTimeoutInterval;
	}

	public long getCheckSessionTimeoutInterval() {
		return this.checkSessionTimeoutInterval;
	}

}
