package com.google.code.yanf4j.config;

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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.util.PropertyUtils;
import com.google.code.yanf4j.util.ResourcesUtils;

/**
 * 配置参数类，默认从classpath下的yanf4j.properties读取参数
 * 
 * @author dennis
 * 
 */
public class Configuration {
	private static final Log log = LogFactory.getLog(Configuration.class);

	public Configuration(String fileName) {
		if (fileName != null) {
			Properties props = null;
			try {
				props = ResourcesUtils.getResourceAsProperties(fileName);
			} catch (IOException e) {
				log
						.error("The config file "
								+ fileName
								+ " is not exist,yanf4j will use the default configuration");
			}
			if (props != null) {
				SessionReadBufferSize = PropertyUtils.getPropertyAsInteger(
						props, "yanf4j.socket.read.buffer.size");
				statisticsServer = PropertyUtils.getPropertyAsBoolean(props,
						"yanf4j.statistics");
				readThreadCount = PropertyUtils.getPropertyAsInteger(props,
						"yanf4j.socket.readthread.count");
				reuseAddress = PropertyUtils.getPropertyAsBoolean(props,
						"yanf4j.socket.reuseaddr");
				tcpRecvBufferSize = PropertyUtils.getPropertyAsInteger(props,
						"yanf4j.socket.recv.buffer.size");
				tcpNoDelay = PropertyUtils.getPropertyAsBoolean(props,
						"yanf4j.socket.nodelay");
				soTimeout = PropertyUtils.getPropertyAsInteger(props,
						"yanf4j.socket.sotimeout");
				CHECK_SESSION_TIMEOUT_INTERVAL = PropertyUtils
						.getPropertyAsLong(props,
								"yanf4j.socket.checktimeout.interval");
				CHECK_SESSION_IDLE_INTERVAL = PropertyUtils
						.getPropertyAsInteger(props,
								"yanf4j.socket.checksessionidle.interval");
			}
		}

	}

	public Configuration() {
		this("yanf4j.properties");
	}

	/**
	 * session读取缓冲区大小
	 */
	private int SessionReadBufferSize = 32 * 1024;

	/**
	 * socket选项 soTimeout
	 */
	private int soTimeout = 0;

	/**
	 * 端口
	 */
	private int port = 0;

	/**
	 * 是否启用统计
	 */
	private boolean statisticsServer = false;
	/**
	 * 是否允许读写并发
	 */
	private boolean handleReadWriteConcurrently = true;
	/**
	 * 处理OP_READ事件线程数
	 */
	private int readThreadCount = 1;

	/**
	 * ByteBuffer增加系数
	 */
	public static final int DEFAULT_INCREASE_BUFF_SIZE = 32 * 1024;

	/**
	 * 检测session过期的周期时间，毫秒
	 */
	public static long CHECK_SESSION_TIMEOUT_INTERVAL = 2000;

	/**
	 * socket选项，SO_REUSEADDR
	 */
	private boolean reuseAddress = false;

	/**
	 * tcp socket选项，套接字接收缓冲区大小
	 */
	private int tcpRecvBufferSize = 16 * 1024;

	/**
	 * tcp socket选项，是否禁止nagle算法
	 */
	private boolean tcpNoDelay = false; // true为禁止Nagle算法

	/**
	 * 超过这个时间没有任何读取或者写操作，即调用handler的onIdle()方法
	 */
	public static int CHECK_SESSION_IDLE_INTERVAL = 2000;

	public int getSessionReadBufferSize() {
		return SessionReadBufferSize;
	}

	public boolean isHandleReadWriteConcurrently() {
		return handleReadWriteConcurrently;
	}

	public int getSoTimeout() {
		return soTimeout;
	}

	// 统计刷新间隔
	protected int statisticsInterval = 5 * 60;

	public int getStatisticsInterval() {
		return statisticsInterval;
	}

	/**
	 * 设置统计数据的刷新间隔，默认是5分钟
	 * 
	 * @param statisticsInterval
	 */
	public void setStatisticsInterval(int statisticsInterval) {
		this.statisticsInterval = statisticsInterval;
	}

	/**
	 * 设置socket SO_TIMEOUT选项，这一选项对于客户端和服务器有不同的含义，具体参见javadoc
	 * 
	 * @param timeout
	 */
	public void setSoTimeout(int timeout) {
		this.soTimeout = timeout;
	}

	/**
	 * 是否允许OP_READ和OP_WRITE事件并发执行，默认为true
	 * 
	 * @param handleReadWriteConcurrently
	 */
	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
	}

	/**
	 * 设置TCP连接session的读取缓冲区大小，注意跟setTcpRecvBufferSize区分
	 * 
	 * @param tcpHandlerReadBufferSize
	 */
	public void setSessionReadBufferSize(int tcpHandlerReadBufferSize) {
		this.SessionReadBufferSize = tcpHandlerReadBufferSize;
	}

	public int getPort() {
		return port;
	}

	/**
	 * 设置监听端口
	 * 
	 * @param tcpPort
	 */
	public void setPort(int tcpPort) {
		this.port = tcpPort;
	}

	public boolean isStatisticsServer() {
		return statisticsServer;
	}

	/**
	 * 启用服务器统计
	 * 
	 * @param statisticsServer
	 *            true即启用统计，false不统计，默认为false
	 */
	public void setStatisticsServer(boolean statisticsServer) {
		this.statisticsServer = statisticsServer;
	}

	public int getReadThreadCount() {
		return readThreadCount;
	}

	/**
	 * 设置处理OP_READ事件线程数，对于UDP来说，此方法不可调用，默认为1
	 * 
	 * @param tcpReadThreadCount
	 */
	public void setReadThreadCount(int tcpReadThreadCount) {
		this.readThreadCount = tcpReadThreadCount;
	}

	public boolean isReuseAddress() {
		return reuseAddress;
	}

	/**
	 * 设置是否启用SO_REUSEADDR选项
	 * 
	 * @param tcpReuseAddress
	 */
	public void setReuseAddress(boolean tcpReuseAddress) {
		this.reuseAddress = tcpReuseAddress;
	}

	/**
	 * 
	 * @return 当前configruation的tcpRecvBufferSize
	 */
	public int getTcpRecvBufferSize() {
		return tcpRecvBufferSize;
	}

	/**
	 * 设置tcp socket的接收缓冲区大小
	 * 
	 * @param tcpRecvBufferSize
	 */
	public void setTcpRecvBufferSize(int tcpRecvBufferSize) {
		this.tcpRecvBufferSize = tcpRecvBufferSize;
	}

	/**
	 * 
	 * @return 是否启用nagle算法
	 */
	public boolean isTcpNoDelay() {
		return tcpNoDelay;
	}

	/**
	 * 设置tcp socket选项，是否禁止nagle算法，true为禁止
	 * 
	 * @param tcp_no_delay
	 */
	public void setTcpNoDelay(boolean tcp_no_delay) {
		tcpNoDelay = tcp_no_delay;
	}

}
