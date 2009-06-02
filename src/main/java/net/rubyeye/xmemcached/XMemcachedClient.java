/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.StatsCommand;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.MemcachedConnector;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;

/**
 * XMemcached客户端API核心类，通过此类的实例与memcached交互。
 * 
 * @author dennis(killme2008@gmail.com)
 * 
 */
public final class XMemcachedClient implements XMemcachedClientMBean,
		MemcachedClient {

	private static final Log log = LogFactory.getLog(XMemcachedClient.class);
	private MemcachedSessionLocator sessionLocator;
	private volatile boolean shutdown;
	private MemcachedConnector connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	private MemcachedHandler memcachedHandler;
	private CommandFactory commandFactory;
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT; // 连接超时

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#setMergeFactor(int)
	 */
	public final void setMergeFactor(final int mergeFactor) {
		if (mergeFactor < 0) {
			throw new IllegalArgumentException("mergeFactor<0");
		}
		this.connector.setMergeFactor(mergeFactor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getConnectTimeout()
	 */
	public long getConnectTimeout() {
		return connectTimeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#setConnectTimeout(long)
	 */
	public void setConnectTimeout(long connectTimeout) {
		if (connectTimeout < 0)
			throw new IllegalArgumentException("connectTimeout<0");
		this.connectTimeout = connectTimeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getConnector()
	 */
	public final MemcachedConnector getConnector() {
		return connector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#setOptimiezeGet(boolean)
	 */
	public final void setOptimiezeGet(final boolean optimiezeGet) {
		this.connector.setOptimiezeGet(optimiezeGet);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#setOptimizeMergeBuffer(boolean)
	 */
	public final void setOptimizeMergeBuffer(final boolean optimizeMergeBuffer) {
		this.connector.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#isShutdown()
	 */
	public final boolean isShutdown() {
		return shutdown;
	}

	@SuppressWarnings("unchecked")
	private final <T> GetsResponse<T> gets0(final String key,
			final byte[] keyBytes, final Transcoder<T> transcoder)
			throws MemcachedException, TimeoutException, InterruptedException {
		GetsResponse<T> result = (GetsResponse<T>) fetch0(key, keyBytes,
				CommandType.GETS_ONE, DEFAULT_OP_TIMEOUT, transcoder);
		return result;
	}

	private final boolean sendCommand(final Command cmd)
			throws MemcachedException {
		if (this.shutdown) {
			throw new IllegalStateException("Xmemcached is stopped");
		}
		return connector.send(cmd);
	}

	/**
	 * XMemcached构造函数
	 * 
	 * @param server
	 *            服务器IP
	 * @param port
	 *            服务器端口
	 * @throws IOException
	 */
	public XMemcachedClient(final String server, final int port)
			throws IOException {
		super();
		checkServerPort(server, port);
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration(),
				new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
		connect(new InetSocketAddress(server, port));
	}

	private void checkServerPort(String server, int port) {
		if (server == null || server.length() == 0) {
			throw new IllegalArgumentException();
		}
		if (port <= 0) {
			throw new IllegalArgumentException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#addServer(java.lang.String,
	 * int)
	 */
	public final void addServer(final String server, final int port)
			throws IOException {
		checkServerPort(server, port);
		connect(new InetSocketAddress(server, port));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#addServer(java.net.InetSocketAddress
	 * )
	 */
	public final void addServer(final InetSocketAddress inetSocketAddress)
			throws IOException {
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		connect(inetSocketAddress);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#addServer(java.lang.String)
	 */
	public final void addServer(String hostList) throws IOException {
		List<InetSocketAddress> addresses = AddrUtil.getAddresses(hostList);
		if (addresses != null && addresses.size() > 0) {
			for (InetSocketAddress address : addresses) {
				connector.connect(address);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getServersDescription()
	 */
	@Override
	public final List<String> getServersDescription() {
		final List<String> result = new ArrayList<String>();
		for (InetSocketAddress socketAddress : this.connector
				.getServerAddresses()) {
			result.add(socketAddress.getHostName() + ":"
					+ socketAddress.getPort());
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#removeServer(java.lang.String)
	 */
	public final void removeServer(String hostList) {
		List<InetSocketAddress> addresses = AddrUtil.getAddresses(hostList);
		if (addresses != null && addresses.size() > 0) {
			for (InetSocketAddress address : addresses) {
				MemcachedTCPSession session = (MemcachedTCPSession) this.connector
						.getSessionByAddress(address);
				if (session != null) {
					// 默认连接断开会自动重连，禁止自动重连
					session.setAllowReconnect(false);
					// 关闭连接
					session.close();
				}
			}
		}
	}

	private void connect(final InetSocketAddress inetSocketAddress)
			throws IOException {
		Future<Boolean> future = null;
		boolean connected = false;
		Throwable throwable = null;
		try {
			future = this.connector.connect(inetSocketAddress);

			if (!future.isDone()
					&& !future.get(this.connectTimeout, TimeUnit.MILLISECONDS)) {
				log.error("connect to " + inetSocketAddress.getHostName() + ":"
						+ inetSocketAddress.getPort() + " fail");
			} else {
				connected = true;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			if (future != null) {
				future.cancel(true);
			}
			throwable = e;
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " error", e);
		} catch (TimeoutException e) {
			if (future != null) {
				future.cancel(true);
			}
			throwable = e;
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " timeout", e);
		} catch (Exception e) {
			if (future != null) {
				future.cancel(true);
			}
			throwable = e;
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " error", e);
		}
		if (!connected) {
			this.connector
					.addToWatingQueue(new MemcachedConnector.ReconnectRequest(
							inetSocketAddress, 0));
			throw new IOException(throwable);

		}
	}

	@SuppressWarnings("unchecked")
	private final <T> Object fetch0(final String key, final byte[] keyBytes,
			final CommandType cmdType, final long timeout,
			Transcoder<T> transcoder) throws InterruptedException,
			TimeoutException, MemcachedException, MemcachedException {
		final Command command = commandFactory.createGetCommand(key, keyBytes,
				cmdType);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, timeout);
		command.getIoBuffer().free(); // free buffer
		checkException(command);
		CachedData data = (CachedData) command.getResult();
		if (data == null) {
			return null;
		}
		if (transcoder == null) {
			transcoder = this.transcoder;
		}
		if (cmdType == CommandType.GETS_ONE) {
			return new GetsResponse<T>(data.getCas(), transcoder.decode(data));
		} else {
			return transcoder.decode(data);
		}
	}

	private final void start0() throws IOException {
		registerMBean();
		startConnector();
	}

	private final void startConnector() throws IOException {
		if (this.shutdown) {
			this.connector.start();
			this.shutdown = false;
		}
	}

	@SuppressWarnings("unchecked")
	private void buildConnector(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration configuration,
			CommandFactory commandFactory, Transcoder transcoder) {
		if (locator == null) {
			locator = new ArrayMemcachedSessionLocator();

		}
		if (allocator == null) {
			allocator = new SimpleBufferAllocator();
		}
		if (configuration == null) {
			configuration = getDefaultConfiguration();
		}
		if (transcoder == null)
			transcoder = new SerializingTranscoder();
		if (commandFactory == null)
			commandFactory = new TextCommandFactory();
		this.commandFactory=commandFactory;
		this.shutdown = true;
		this.transcoder = transcoder;
		this.sessionLocator = locator;
		this.connector = new MemcachedConnector(configuration, sessionLocator,
				allocator);
		this.connector.setSendBufferSize(DEFAULT_TCP_SEND_BUFF_SIZE);
		this.memcachedHandler = new MemcachedHandler(this);
		this.connector.setHandler(memcachedHandler);
		this.connector.setCodecFactory(new MemcachedCodecFactory());
	}

	private final void registerMBean() {
		if (this.shutdown)
			XMemcachedMbeanServer.getInstance().registMBean(
					this,
					this.getClass().getPackage().getName() + ":type="
							+ this.getClass().getSimpleName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#setBufferAllocator(net.rubyeye
	 * .xmemcached.buffer.BufferAllocator)
	 */
	public final void setBufferAllocator(final BufferAllocator bufferAllocator) {
		this.connector.setBufferAllocator( bufferAllocator);
	}

	/**
	 * XMemcached构造函数
	 * 
	 * @param configuration
	 *            yanf4j网络参数配置
	 * @throws IOException
	 */
	public XMemcachedClient(final Configuration configuration)
			throws IOException {
		this(new ArrayMemcachedSessionLocator(), new SimpleBufferAllocator(),
				configuration);

	}

	/**
	 * XMemcached构造函数
	 * 
	 * @param configuration
	 *            yanf4j网络参数配置
	 * @param allocator
	 *            ByteBuffer分配器
	 * @throws IOException
	 */
	public XMemcachedClient(final Configuration configuration,
			final BufferAllocator allocator) throws IOException {
		this(new ArrayMemcachedSessionLocator(), allocator, configuration);

	}

	/**
	 * XMemcached构造函数
	 * 
	 * @param configuration
	 *            yanf4j网络参数配置
	 * @param allocator
	 *            ByteBuffer分配器
	 * @param locator
	 *            连接查找器，采用余数分布或者一致性哈希
	 * @throws IOException
	 */
	public XMemcachedClient(final Configuration configuration,
			final MemcachedSessionLocator locator) throws IOException {
		this(locator, new SimpleBufferAllocator(), configuration);

	}

	/**
	 * 返回默认yanf4j网络参数配置
	 * 
	 * @return
	 */
	public static final Configuration getDefaultConfiguration() {
		final Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(DEFAULT_TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(DEFAULT_SESSION_READ_BUFF_SIZE);
		configuration.setTcpNoDelay(DEFAULT_TCP_NO_DELAY);
		configuration.setReadThreadCount(DEFAULT_READ_THREAD_COUNT);
		return configuration;
	}

	/**
	 * XMemcached构造函数
	 * 
	 * @param inetSocketAddress
	 *            服务器IP地址
	 * @throws IOException
	 */
	public XMemcachedClient(final InetSocketAddress inetSocketAddress)
			throws IOException {
		super();
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration(),
				new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
		connect(inetSocketAddress);
	}

	public XMemcachedClient() throws IOException {
		super();
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration(),
				new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
	}

	public XMemcachedClient(MemcachedSessionLocator locator) throws IOException {
		this(locator, new SimpleBufferAllocator(), getDefaultConfiguration());
	}

	@SuppressWarnings("unchecked")
	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory, Transcoder transcoder)
			throws IOException {
		super();
		buildConnector(locator, allocator, conf, commandFactory, transcoder);
		start0();
	}

	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory) throws IOException {
		this(locator, allocator, conf, commandFactory,
				new SerializingTranscoder());
	}

	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf) throws IOException {
		this(locator, allocator, conf, new TextCommandFactory(),
				new SerializingTranscoder());
	}

	@SuppressWarnings("unchecked")
	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList) throws IOException {
		super();
		buildConnector(locator, allocator, conf, commandFactory, transcoder);
		start0();
		if (addressList != null) {
			for (InetSocketAddress inetSocketAddress : addressList) {
				connect(inetSocketAddress);
			}
		}
	}

	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			List<InetSocketAddress> addressList) throws IOException {
		this(locator, allocator, conf, new TextCommandFactory(),
				new SerializingTranscoder(), addressList);
	}

	public XMemcachedClient(BufferAllocator allocator) throws IOException {
		this(new ArrayMemcachedSessionLocator(), allocator,
				getDefaultConfiguration());

	}

	public XMemcachedClient(List<InetSocketAddress> addressList)
			throws IOException {
		super();
		if (addressList == null || addressList.isEmpty())
			throw new IllegalArgumentException("Empty address list");
		BufferAllocator simpleBufferAllocator = new SimpleBufferAllocator();
		buildConnector(new ArrayMemcachedSessionLocator(),
				simpleBufferAllocator, getDefaultConfiguration(),
				new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
		for (InetSocketAddress inetSocketAddress : addressList) {
			connect(inetSocketAddress);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String, long,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (T) get0(key, timeout, CommandType.GET_ONE, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String, long)
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return (T) get(key, timeout, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> T get(final String key, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(key, DEFAULT_OP_TIMEOUT, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (T) get(key, DEFAULT_OP_TIMEOUT);
	}

	private <T> Object get0(final String key, final long timeout,
			final CommandType cmdType, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		return fetch0(key, keyBytes, cmdType, timeout, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String, long,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (GetsResponse<T>) get0(key, timeout, CommandType.GETS_ONE,
				transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String)
	 */
	public final <T> GetsResponse<T> gets(final String key)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(key, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String, long)
	 */
	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(key, timeout, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key,
			final Transcoder transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(key, DEFAULT_OP_TIMEOUT, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection,
	 * long, net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return getMulti0(keyCollections, timeout, CommandType.GET_MANY,
				transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> Map<String, T> get(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return getMulti0(keyCollections, DEFAULT_OP_TIMEOUT,
				CommandType.GET_MANY, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection)
	 */
	public final <T> Map<String, T> get(final Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(keyCollections, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection,
	 * long)
	 */
	@SuppressWarnings("unchecked")
	public final <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return (Map<String, T>) get(keyCollections, timeout, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection,
	 * long, net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	@SuppressWarnings("unchecked")
	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (Map<String, GetsResponse<T>>) getMulti0(keyCollections,
				timeout, CommandType.GETS_MANY, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection)
	 */
	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(keyCollections, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection,
	 * long)
	 */
	@SuppressWarnings("unchecked")
	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(keyCollections, timeout, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(keyCollections, DEFAULT_OP_TIMEOUT, transcoder);
	}

	private final <T> Map<String, T> getMulti0(
			final Collection<String> keyCollections, final long timeout,
			final CommandType cmdType, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		if (keyCollections == null || keyCollections.size() == 0) {
			return null;
		}
		final CountDownLatch latch;
		final List<Command> commands;
		if (this.connector.getSessionSet().size() <= 1) {
			commands = new ArrayList<Command>(1);
			latch = new CountDownLatch(1);
			commands.add(sendGetMultiCommand(keyCollections, latch, cmdType,
					transcoder));

		} else {
			Collection<List<String>> catalogKeys = catalogKeys(keyCollections);
			commands = new ArrayList<Command>(catalogKeys.size());
			latch = new CountDownLatch(catalogKeys.size());
			for (List<String> keys : catalogKeys) {
				commands.add(sendGetMultiCommand(keys, latch, cmdType,
						transcoder));
			}
		}
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			for (Command getCmd : commands) {
				getCmd.cancel();
			}
			throw new TimeoutException("Timed out waiting for operation");
		}
		return reduceResult(cmdType, transcoder, commands);
	}

	@SuppressWarnings("unchecked")
	private <T> Map<String, T> reduceResult(final CommandType cmdType,
			final Transcoder<T> transcoder, final List<Command> commands)
			throws MemcachedException {
		final Map<String, T> result = new HashMap<String, T>(commands.size());
		for (Command getCmd : commands) {
			getCmd.getIoBuffer().free();
			checkException(getCmd);
			Map<String, CachedData> map = (Map<String, CachedData>) getCmd
					.getResult();
			if (cmdType == CommandType.GET_MANY) {

				Iterator<Map.Entry<String, CachedData>> it = map.entrySet()
						.iterator();
				while (it.hasNext()) {
					Map.Entry<String, CachedData> entry = it.next();
					result.put(entry.getKey(), transcoder.decode(entry
							.getValue()));
				}

			} else {
				Iterator<Map.Entry<String, CachedData>> it = map.entrySet()
						.iterator();
				while (it.hasNext()) {
					Map.Entry<String, CachedData> entry = it.next();
					GetsResponse getsResponse = new GetsResponse(entry
							.getValue().getCas(), transcoder.decode(entry
							.getValue()));
					result.put(entry.getKey(), (T) getsResponse);
				}

			}

		}
		return result;
	}

	/**
	 * 对key按照hash值进行分类，发送到不同节点
	 * 
	 * @param keyCollections
	 * @return
	 */
	private final Collection<List<String>> catalogKeys(
			final Collection<String> keyCollections) {
		final Map<Session, List<String>> catalogMap = new HashMap<Session, List<String>>();

		for (String key : keyCollections) {
			Session index = this.sessionLocator.getSessionByKey(key);
			if (!catalogMap.containsKey(index)) {
				List<String> tmpKeys = new ArrayList<String>(100);
				tmpKeys.add(key);
				catalogMap.put(index, tmpKeys);
			} else {
				catalogMap.get(index).add(key);
			}
		}

		Collection<List<String>> catalogKeys = catalogMap.values();
		return catalogKeys;
	}

	private final <T> Command sendGetMultiCommand(
			final Collection<String> keys, final CountDownLatch latch,
			final CommandType cmdType, final Transcoder<T> transcoder)
			throws InterruptedException, TimeoutException, MemcachedException {
		final Command command = commandFactory.createGetMultiCommand(keys,
				latch, cmdType, transcoder);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		return command;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long)
	 */
	public final <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendStoreCommand(key, exp, value, CommandType.SET, "set",
				timeout, -1, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int,
	 * java.lang.Object)
	 */
	public final boolean set(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return set(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int,
	 * java.lang.Object, long)
	 */
	@SuppressWarnings("unchecked")
	public final boolean set(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return set(key, exp, value, this.transcoder, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return set(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long)
	 */
	public final <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendStoreCommand(key, exp, value, CommandType.SET, "add",
				timeout, -1, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int,
	 * java.lang.Object)
	 */
	public final boolean add(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int,
	 * java.lang.Object, long)
	 */
	@SuppressWarnings("unchecked")
	public final boolean add(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return add(key, exp, value, this.transcoder, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String,
	 * int, T, net.rubyeye.xmemcached.transcoders.Transcoder, long)
	 */
	public final <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendStoreCommand(key, exp, value, CommandType.SET, "replace",
				timeout, -1, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String,
	 * int, java.lang.Object)
	 */
	public final boolean replace(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException {
		return replace(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String,
	 * int, java.lang.Object, long)
	 */
	@SuppressWarnings("unchecked")
	public final boolean replace(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return replace(key, exp, value, this.transcoder, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String,
	 * int, T, net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return replace(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String,
	 * java.lang.Object)
	 */
	public final boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return append(key, value, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String,
	 * java.lang.Object, long)
	 */
	@SuppressWarnings("unchecked")
	public final boolean append(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {

		return sendStoreCommand(key, 0, value, CommandType.APPEND, "append",
				timeout, -1, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String,
	 * java.lang.Object)
	 */
	public final boolean prepend(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return prepend(key, value, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String,
	 * java.lang.Object, long)
	 */
	@SuppressWarnings("unchecked")
	public final boolean prepend(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return sendStoreCommand(key, 0, value, CommandType.PREPEND, "prepend",
				timeout, -1, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
	 * java.lang.Object, long)
	 */
	public final boolean cas(final String key, final int exp,
			final Object value, final long cas) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, value, DEFAULT_OP_TIMEOUT, cas);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long, long)
	 */
	public final <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout,
			final long cas) throws TimeoutException, InterruptedException,
			MemcachedException {
		return sendStoreCommand(key, exp, value, CommandType.CAS, "cas",
				timeout, cas, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
	 * java.lang.Object, long, long)
	 */
	@SuppressWarnings("unchecked")
	public final boolean cas(final String key, final int exp,
			final Object value, final long timeout, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		return cas(key, exp, value, this.transcoder, timeout, cas);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long)
	 */
	public final <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		return cas(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT, cas);
	}

	private final <T> boolean cas0(final String key, final int exp,
			GetsResponse<T> getsResponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder, byte[] keyBytes)
			throws TimeoutException, InterruptedException, MemcachedException {
		if (operation == null) {
			throw new IllegalArgumentException("CASOperation could not be null");
		}
		if (operation.getMaxTries() < 0) {
			throw new IllegalArgumentException(
					"max tries must be greater than 0");
		}
		int tryCount = 0;
		GetsResponse<T> result = getsResponse;
		if (result == null) {
			throw new MemcachedException("Null GetsResponse");
		}
		while (tryCount < operation.getMaxTries()
				&& result != null
				&& !sendStoreCommand(key, exp, operation.getNewValue(result
						.getCas(), result.getValue()), CommandType.CAS, "cas",
						DEFAULT_OP_TIMEOUT, result.getCas(), transcoder)) {
			tryCount++;
			result = gets0(key, keyBytes, transcoder);
			if (result == null) {
				throw new MemcachedException(
						"could not gets the value for Key=" + key);
			}
			if (tryCount >= operation.getMaxTries()) {
				throw new TimeoutException("CAS try times is greater than max");
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
	 * net.rubyeye.xmemcached.CASOperation,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		GetsResponse<T> result = gets0(key, keyBytes, transcoder);
		return cas0(key, exp, result, operation, transcoder, keyBytes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
	 * net.rubyeye.xmemcached.GetsResponse, net.rubyeye.xmemcached.CASOperation,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		return cas0(key, exp, getsReponse, operation, transcoder, keyBytes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
	 * net.rubyeye.xmemcached.GetsResponse, net.rubyeye.xmemcached.CASOperation)
	 */
	@SuppressWarnings("unchecked")
	public final <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException {
		return cas(key, exp, getsReponse, operation, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String,
	 * net.rubyeye.xmemcached.GetsResponse, net.rubyeye.xmemcached.CASOperation)
	 */
	public final <T> boolean cas(final String key, GetsResponse<T> getsReponse,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, 0, getsReponse, operation);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
	 * net.rubyeye.xmemcached.CASOperation)
	 */
	@SuppressWarnings("unchecked")
	public final <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, operation, this.transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String,
	 * net.rubyeye.xmemcached.CASOperation)
	 */
	public final <T> boolean cas(final String key,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, 0, operation);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#delete(java.lang.String, int)
	 */
	public final boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = commandFactory.createDeleteCommand(key,
				keyBytes, time);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT);
		command.getIoBuffer().free();
		checkException(command);
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Boolean) command.getResult();
	}

	private void checkException(final Command command)
			throws MemcachedException {
		if (command.getException() != null) {
			throw new MemcachedException(command.getException());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#version()
	 */
	public final String version() throws TimeoutException,
			InterruptedException, MemcachedException {
		final Command command = commandFactory.createVersionCommand();
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT);
		command.getIoBuffer().free(); // free buffer
		checkException(command);
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (String) command.getResult();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#incr(java.lang.String, int)
	 */
	public final int incr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendIncrOrDecrCommand(key, num, CommandType.INCR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#decr(java.lang.String, int)
	 */
	public final int decr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendIncrOrDecrCommand(key, num, CommandType.DECR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll()
	 */
	public final void flushAll() throws TimeoutException, InterruptedException,
			MemcachedException {
		flushAll(DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(long)
	 */
	public final void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		final Collection<Session> sessions = this.connector.getSessionSet();
		CountDownLatch latch = new CountDownLatch(sessions.size());
		for (Session session : sessions) {
			if (session != null && !session.isClosed()) {
				Command command = commandFactory.createFlushAllCommand(latch);
				session.send(command);
			} else
				latch.countDown();
		}
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.lang.String,
	 * long)
	 */
	public final void flushAll(String server, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		InetSocketAddress address = AddrUtil.getOneAddress(server);
		flushAll(address, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#flushAll(java.net.InetSocketAddress
	 * )
	 */
	public final void flushAll(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException {
		flushAll(address, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#flushAll(java.net.InetSocketAddress
	 * , long)
	 */
	public final void flushAll(InetSocketAddress address, long timeout)
			throws MemcachedException, InterruptedException, TimeoutException {
		if (address == null)
			throw new IllegalArgumentException("Null adderss");
		CountDownLatch latch = new CountDownLatch(1);

		Session session = this.connector.getSessionByAddress(address);
		if (session == null)
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		Command command = commandFactory.createFlushAllCommand(latch);
		if (!session.send(command))
			throw new MemcachedException("send command fail");
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.lang.String)
	 */
	public final void flushAll(String host) throws TimeoutException,
			InterruptedException, MemcachedException {
		flushAll(host, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#stats(java.lang.String, long)
	 */
	public final Map<String, String> stats(String host, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		InetSocketAddress address = AddrUtil.getOneAddress(host);
		return stats(address, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#stats(java.lang.String)
	 */
	public final Map<String, String> stats(String host)
			throws TimeoutException, InterruptedException, MemcachedException {
		return stats(host, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#stats(java.net.InetSocketAddress)
	 */
	public final Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException {
		return stats(address, DEFAULT_OP_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#stats(java.net.InetSocketAddress,
	 * long)
	 */
	@SuppressWarnings("unchecked")
	public final Map<String, String> stats(InetSocketAddress address,
			long timeout) throws MemcachedException, InterruptedException,
			TimeoutException {
		if (address == null)
			throw new IllegalArgumentException("Null inetSocketAddress");
		CountDownLatch latch = new CountDownLatch(1);

		Session session = this.connector.getSessionByAddress(address);
		if (session == null)
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		Command command = commandFactory.createStatsCommand(address, latch);
		if (!session.send(command))
			throw new MemcachedException("send command fail");
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		return (Map<String, String>) command.getResult();
	}

	@Override
	public Map<InetSocketAddress, Map<String, String>> stats()
			throws MemcachedException, InterruptedException, TimeoutException {
		return stats(DEFAULT_OP_TIMEOUT);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<InetSocketAddress, Map<String, String>> stats(long timeout)
			throws MemcachedException, InterruptedException, TimeoutException {
		final Set<Session> sessionSet = this.connector.getSessionSet();
		Map<InetSocketAddress, Map<String, String>> collectResult = new HashMap<InetSocketAddress, Map<String, String>>();
		final CountDownLatch latch = new CountDownLatch(sessionSet.size());
		List<Command> commands = new ArrayList<Command>(sessionSet.size());
		for (Session session : sessionSet) {
			Command command = commandFactory.createStatsCommand(session
					.getRemoteSocketAddress(), latch);
			if (!session.send(command))
				throw new MemcachedException("send command fail");
			commands.add(command);

		}
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		for (Command command : commands) {
			checkException(command);
			collectResult.put(((StatsCommand) command).getServer(),
					(Map<String, String>) command.getResult());
		}
		return collectResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#shutdown()
	 */
	public final void shutdown() throws IOException {
		if (this.shutdown) {
			return;
		}
		this.shutdown = true;
		this.connector.stop();
		XMemcachedMbeanServer.getInstance().shutdown();
	}

	private final int sendIncrOrDecrCommand(final String key, final int num,
			final CommandType cmdType) throws InterruptedException,
			TimeoutException, MemcachedException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = commandFactory.createIncrDecrCommand(key,
				keyBytes, num, cmdType);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT);
		command.getIoBuffer().free();
		checkException(command);
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Integer) command.getResult();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#delete(java.lang.String)
	 */
	public final boolean delete(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return delete(key, 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getTranscoder()
	 */
	@SuppressWarnings("unchecked")
	public final Transcoder getTranscoder() {
		return transcoder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#setTranscoder(net.rubyeye.xmemcached
	 * .transcoders.Transcoder)
	 */
	@SuppressWarnings("unchecked")
	public final void setTranscoder(final Transcoder transcoder) {
		this.transcoder = transcoder;
	}

	@SuppressWarnings("unchecked")
	private <T> boolean sendStoreCommand(final String key, final int exp,
			final T value, final CommandType cmdType, final String cmd,
			final long timeout, final long cas, Transcoder<T> transcoder)
			throws InterruptedException, TimeoutException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		if (value == null) {
			throw new IllegalArgumentException("value could not be null");
		}
		if (exp < 0) {
			throw new IllegalArgumentException(
					"Expire time must be greater than 0");
		}
		if (transcoder == null) {
			transcoder = this.transcoder;
		}
		final Command command = commandFactory.createStoreCommand(key,
				keyBytes, exp, value, cmdType, cmd, cas, transcoder);

		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, timeout);
		command.getIoBuffer().free();
		checkException(command);
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Boolean) command.getResult();
	}

	private void latchWait(final Command cmd, final long timeout)
			throws InterruptedException, TimeoutException {
		if (!cmd.getLatch().await(timeout, TimeUnit.MILLISECONDS)) {
			cmd.cancel();
			throw new TimeoutException("Timed out waiting for operation");
		}
	}
}
