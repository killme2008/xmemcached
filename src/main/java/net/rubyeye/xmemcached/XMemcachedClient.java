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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.StatsCommand;
import net.rubyeye.xmemcached.command.VersionCommand;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.MemcachedClientStateListenerAdapter;
import net.rubyeye.xmemcached.impl.MemcachedConnector;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;

/**
 * Memcached Client for connecting to memcached server and do operations.
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
	private long opTimeout = DEFAULT_OP_TIMEOUT;
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT; // 连接超时

	private CopyOnWriteArrayList<MemcachedClientStateListenerAdapter> stateListenerAdapters = new CopyOnWriteArrayList<MemcachedClientStateListenerAdapter>();

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
		return this.connectTimeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#setConnectTimeout(long)
	 */
	public void setConnectTimeout(long connectTimeout) {
		if (connectTimeout < 0) {
			throw new IllegalArgumentException("connectTimeout<0");
		}
		this.connectTimeout = connectTimeout;
	}

	/**
	 * get operation timeout setting
	 * 
	 * @return
	 */
	public final long getOpTimeout() {
		return this.opTimeout;
	}

	/**
	 * set operation timeout,default is one second.
	 * 
	 * @param opTimeout
	 */
	public final void setOpTimeout(long opTimeout) {
		if (opTimeout < 0) {
			throw new IllegalArgumentException("opTimeout<0");
		}
		this.opTimeout = opTimeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getConnector()
	 */
	public final MemcachedConnector getConnector() {
		return this.connector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#setOptimiezeGet(boolean)
	 */
	public final void setOptimiezeGet(final boolean optimiezeGet) {
		this.connector.setOptimizeGet(optimiezeGet);
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
		return this.shutdown;
	}

	@SuppressWarnings("unchecked")
	private final <T> GetsResponse<T> gets0(final String key,
			final byte[] keyBytes, final Transcoder<T> transcoder)
			throws MemcachedException, TimeoutException, InterruptedException {
		GetsResponse<T> result = (GetsResponse<T>) fetch0(key, keyBytes,
				CommandType.GETS_ONE, this.opTimeout, transcoder);
		return result;
	}

	private final void sendCommand(final Command cmd) throws MemcachedException {
		if (this.shutdown) {
			throw new IllegalStateException("Xmemcached is stopped");
		}
		this.connector.send(cmd);
	}

	/**
	 * XMemcached constructor,default weight is 1
	 * 
	 * @param server
	 *            服务器IP
	 * @param port
	 *            服务器端口
	 * @throws IOException
	 */
	public XMemcachedClient(final String server, final int port)
			throws IOException {
		this(server, port, 1);
	}

	/**
	 * XMemcached constructor
	 * 
	 * @param server
	 *            server host
	 * @param port
	 *            server port
	 * @param weight
	 *            server weight
	 * @throws IOException
	 */
	public XMemcachedClient(final String server, final int port, int weight)
			throws IOException {
		super();
		if (weight <= 0) {
			throw new IllegalArgumentException("weight<=0");
		}
		checkServerPort(server, port);
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration(),
				new TextCommandFactory(), new SerializingTranscoder());
		start0();
		connect(new InetSocketAddress(server, port), weight);
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
		addServer(server, port, 1);
	}

	/**
	 * add a memcached server to MemcachedClient
	 * 
	 * @param server
	 * @param port
	 * @param weight
	 * @throws IOException
	 */
	public final void addServer(final String server, final int port, int weight)
			throws IOException {
		if (weight <= 0) {
			throw new IllegalArgumentException("weight<=0");
		}
		checkServerPort(server, port);
		connect(new InetSocketAddress(server, port), weight);
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
		addServer(inetSocketAddress, 1);
	}

	public final void addServer(final InetSocketAddress inetSocketAddress,
			int weight) throws IOException {
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException("Null InetSocketAddress");
		}
		if (weight <= 0) {
			throw new IllegalArgumentException("weight<=0");
		}
		connect(inetSocketAddress, weight);
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
				this.connector.connect(address, 1);
			}
		}
	}

	@Override
	public void addOneServerWithWeight(String server, int weight)
			throws IOException {
		InetSocketAddress address = AddrUtil.getOneAddress(server);
		if (address == null) {
			throw new IllegalArgumentException("Null Server");
		}
		if (weight <= 0) {
			throw new IllegalArgumentException("weight<=0");
		}
		this.connector.connect(address, weight);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getServersDescription()
	 */
	@Override
	public final List<String> getServersDescription() {
		final List<String> result = new ArrayList<String>();
		for (Session session : this.connector.getSessionSet()) {
			InetSocketAddress socketAddress = session.getRemoteSocketAddress();
			int weight = ((MemcachedTCPSession) session).getWeight();
			result.add(socketAddress.getHostName() + ":"
					+ socketAddress.getPort() + "(weight=" + weight + ")");
		}
		return result;
	}

	public final void setServerWeight(String server, int weight) {
		InetSocketAddress socketAddress = AddrUtil.getOneAddress(server);
		Session session = this.connector.getSessionByAddress(socketAddress);
		if (session == null) {
			throw new IllegalArgumentException("There is no server " + server);
		}
		((MemcachedTCPSession) session).setWeight(weight);
		this.connector.updateSessions();
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

	private void connect(final InetSocketAddress inetSocketAddress, int weight)
			throws IOException {
		Future<Boolean> future = null;
		boolean connected = false;
		Throwable throwable = null;
		try {
			future = this.connector.connect(inetSocketAddress, weight);

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
		// If it is not connected,it will be added to waiting queue for
		// reconnecting.
		if (!connected) {
			this.connector.addToWatingQueue(new ReconnectRequest(
					inetSocketAddress, 0, weight));
			log.error("Connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " fail", throwable);
			// throw new IOException(throwable);
		}
	}

	@SuppressWarnings("unchecked")
	private final <T> Object fetch0(final String key, final byte[] keyBytes,
			final CommandType cmdType, final long timeout,
			Transcoder<T> transcoder) throws InterruptedException,
			TimeoutException, MemcachedException, MemcachedException {
		final Command command = this.commandFactory.createGetCommand(key,
				keyBytes, cmdType);
		sendCommand(command);
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
		if (transcoder == null) {
			transcoder = new SerializingTranscoder();
		}
		if (commandFactory == null) {
			commandFactory = new TextCommandFactory();
		}
		this.commandFactory = commandFactory;
		this.shutdown = true;
		this.transcoder = transcoder;
		this.sessionLocator = locator;
		this.connector = new MemcachedConnector(configuration,
				this.sessionLocator, allocator);
		this.connector.setSendBufferSize(DEFAULT_TCP_SEND_BUFF_SIZE);
		this.memcachedHandler = new MemcachedHandler(this);
		this.connector.setHandler(this.memcachedHandler);
		this.connector.setCodecFactory(new MemcachedCodecFactory());
	}

	private final void registerMBean() {
		if (this.shutdown) {
			XMemcachedMbeanServer.getInstance().registMBean(
					this,
					this.getClass().getPackage().getName() + ":type="
							+ this.getClass().getSimpleName());
		}
	}

	@Override
	public void setOptimizeGet(boolean optimizeGet) {
		setOptimiezeGet(optimizeGet);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#setBufferAllocator(net.rubyeye
	 * .xmemcached.buffer.BufferAllocator)
	 */
	public final void setBufferAllocator(final BufferAllocator bufferAllocator) {
		this.connector.setBufferAllocator(bufferAllocator);
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
	 * Get default network configration for xmemcached.
	 * 
	 * @return
	 */
	public static final Configuration getDefaultConfiguration() {
		final Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(DEFAULT_TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(DEFAULT_SESSION_READ_BUFF_SIZE);
		configuration.setTcpNoDelay(DEFAULT_TCP_NO_DELAY);
		configuration.setReadThreadCount(DEFAULT_READ_THREAD_COUNT);
		configuration.setSessionIdleTimeout(0);
		configuration.setWriteThreadCount(Runtime.getRuntime()
				.availableProcessors());
		configuration.setCheckSessionTimeoutInterval(0);
		return configuration;
	}

	/**
	 * XMemcached Constructor.
	 * 
	 * @param inetSocketAddress
	 * @param weight
	 * @throws IOException
	 */
	public XMemcachedClient(final InetSocketAddress inetSocketAddress,
			int weight) throws IOException {
		super();
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException("Null InetSocketAddress");

		}
		if (weight <= 0) {
			throw new IllegalArgumentException("weight<=0");
		}
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration(),
				new TextCommandFactory(), new SerializingTranscoder());
		start0();
		connect(inetSocketAddress, weight);
	}

	public XMemcachedClient(final InetSocketAddress inetSocketAddress)
			throws IOException {
		this(inetSocketAddress, 1);
	}

	public XMemcachedClient() throws IOException {
		super();
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration(),
				new TextCommandFactory(), new SerializingTranscoder());
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

	/**
	 * XMemcachedClient constructor.Every server's weight is one by default.
	 * 
	 * @param locator
	 * @param allocator
	 * @param conf
	 * @param commandFactory
	 * @param transcoder
	 * @param addressList
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList) throws IOException {
		super();
		optimiezeSetReadThreadCount(conf, addressList);
		buildConnector(locator, allocator, conf, commandFactory, transcoder);
		start0();
		if (addressList != null) {
			for (InetSocketAddress inetSocketAddress : addressList) {
				connect(inetSocketAddress, 1);
			}
		}
	}

	/**
	 * XMemcachedClient constructor.Every server's weight is one by default.
	 * 
	 * @param locator
	 * @param allocator
	 * @param conf
	 * @param commandFactory
	 * @param transcoder
	 * @param addressList
	 * @param stateListeners
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList,
			List<MemcachedClientStateListener> stateListeners)
			throws IOException {
		super();
		optimiezeSetReadThreadCount(conf, addressList);
		buildConnector(locator, allocator, conf, commandFactory, transcoder);
		if (stateListeners != null) {
			for (MemcachedClientStateListener stateListener : stateListeners) {
				addStateListener(stateListener);
			}
		}
		start0();
		if (addressList != null) {
			for (InetSocketAddress inetSocketAddress : addressList) {
				connect(inetSocketAddress, 1);
			}
		}
	}

	/**
	 * XMemcachedClient constructor.
	 * 
	 * @param locator
	 * @param allocator
	 * @param conf
	 * @param commandFactory
	 * @param transcoder
	 * @param addressList
	 * @param weights
	 *            weight array for address list
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList, int[] weights)
			throws IOException {
		this(locator, allocator, conf, commandFactory, transcoder, addressList,
				weights, null);
	}

	/**
	 * XMemcachedClient constructor.
	 * 
	 * @param locator
	 * @param allocator
	 * @param conf
	 * @param commandFactory
	 * @param transcoder
	 * @param addressList
	 * @param weights
	 * @param stateListeners
	 *            weight array for address list
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList, int[] weights,
			List<MemcachedClientStateListener> stateListeners)
			throws IOException {
		super();
		if (weights == null && addressList != null) {
			throw new IllegalArgumentException("Null weights");
		}
		if (weights != null && addressList == null) {
			throw new IllegalArgumentException("Null addressList");
		}

		if (weights != null) {
			for (int weight : weights) {
				if (weight <= 0) {
					throw new IllegalArgumentException("Some weights<=0");
				}
			}
		}
		if (weights != null && addressList != null
				&& weights.length < addressList.size()) {
			throw new IllegalArgumentException(
					"weights.length is less than addressList.size()");
		}
		optimiezeSetReadThreadCount(conf, addressList);
		buildConnector(locator, allocator, conf, commandFactory, transcoder);
		if (stateListeners != null) {
			for (MemcachedClientStateListener stateListener : stateListeners) {
				addStateListener(stateListener);
			}
		}
		start0();
		if (addressList != null && weights != null) {
			for (int i = 0; i < addressList.size(); i++) {
				connect(addressList.get(i), weights[i]);
			}
		}
	}

	private final void optimiezeSetReadThreadCount(Configuration conf,
			List<InetSocketAddress> addressList) {
		if (conf != null && addressList != null) {
			if (isLinuxPlatform() && addressList.size() > 1
					&& conf.getReadThreadCount() == DEFAULT_READ_THREAD_COUNT) {
				int cpus = Runtime.getRuntime().availableProcessors();
				conf
						.setReadThreadCount(addressList.size() > cpus + 1 ? cpus + 1
								: addressList.size());
			}
		}
	}

	private final boolean isLinuxPlatform() {
		String osName = System.getProperty("os.name");
		if (osName != null && osName.toLowerCase().indexOf("linux") >= 0) {
			return true;
		} else {
			return false;
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

	/**
	 * XMemcached Constructor.Every server's weight is one by default.
	 * 
	 * @param addressList
	 * @throws IOException
	 */
	public XMemcachedClient(List<InetSocketAddress> addressList)
			throws IOException {
		super();
		if (addressList == null || addressList.isEmpty()) {
			throw new IllegalArgumentException("Empty address list");
		}
		BufferAllocator simpleBufferAllocator = new SimpleBufferAllocator();
		buildConnector(new ArrayMemcachedSessionLocator(),
				simpleBufferAllocator, getDefaultConfiguration(),
				new TextCommandFactory(), new SerializingTranscoder());
		start0();
		for (InetSocketAddress inetSocketAddress : addressList) {
			connect(inetSocketAddress, 1);
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
		return get(key, this.opTimeout, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (T) get(key, this.opTimeout);
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
		return gets(key, this.opTimeout);
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
		return gets(key, this.opTimeout, transcoder);
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
		return getMulti0(keyCollections, this.opTimeout, CommandType.GET_MANY,
				transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection)
	 */
	public final <T> Map<String, T> get(final Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(keyCollections, this.opTimeout);
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
		return get(keyCollections, timeout, this.transcoder);
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
		return gets(keyCollections, this.opTimeout);
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
		return gets(keyCollections, this.opTimeout, transcoder);
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
		final Command command = this.commandFactory.createGetMultiCommand(keys,
				latch, cmdType, transcoder);
		sendCommand(command);
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
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		return sendStoreCommand(this.commandFactory.createSetCommand(key,
				keyBytes, exp, value, false, transcoder), timeout);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setWithNoReply(String key, int exp, Object value)
			throws InterruptedException, MemcachedException {
		setWithNoReply(key, exp, value, this.transcoder);
	}

	@Override
	public <T> void setWithNoReply(String key, int exp, T value,
			Transcoder<T> transcoder) throws InterruptedException,
			MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		try {
			sendStoreCommand(this.commandFactory.createSetCommand(key,
					keyBytes, exp, value, true, transcoder), this.opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	private final <T> byte[] checkStoreArguments(final String key,
			final int exp, final T value) {
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		if (value == null) {
			throw new IllegalArgumentException("value could not be null");
		}
		if (exp < 0) {
			throw new IllegalArgumentException(
					"Expire time must be greater than 0");
		}
		return keyBytes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int,
	 * java.lang.Object)
	 */
	public final boolean set(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return set(key, exp, value, this.opTimeout);
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
		return set(key, exp, value, transcoder, this.opTimeout);
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
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		return sendStoreCommand(this.commandFactory.createAddCommand(key,
				keyBytes, exp, value, false, transcoder), timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int,
	 * java.lang.Object)
	 */
	public final boolean add(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, this.opTimeout);
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
		return add(key, exp, value, transcoder, this.opTimeout);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addWithNoReply(String key, int exp, Object value)
			throws InterruptedException, MemcachedException {
		addWithNoReply(key, exp, value, this.transcoder);

	}

	@Override
	public <T> void addWithNoReply(String key, int exp, T value,
			Transcoder<T> transcoder) throws InterruptedException,
			MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		try {
			sendStoreCommand(this.commandFactory.createAddCommand(key,
					keyBytes, exp, value, true, transcoder), this.opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public void replaceWithNoReply(String key, int exp, Object value)
			throws InterruptedException, MemcachedException {
		replaceWithNoReply(key, exp, value, this.transcoder);

	}

	@Override
	public <T> void replaceWithNoReply(String key, int exp, T value,
			Transcoder<T> transcoder) throws InterruptedException,
			MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		try {
			sendStoreCommand(this.commandFactory.createReplaceCommand(key,
					keyBytes, exp, value, true, transcoder), this.opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}

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
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		return sendStoreCommand(this.commandFactory.createReplaceCommand(key,
				keyBytes, exp, value, false, transcoder), timeout);
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
		return replace(key, exp, value, this.opTimeout);
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
		return replace(key, exp, value, transcoder, this.opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String,
	 * java.lang.Object)
	 */
	public final boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return append(key, value, this.opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String,
	 * java.lang.Object, long)
	 */
	public final boolean append(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		return sendStoreCommand(this.commandFactory.createAppendCommand(key,
				keyBytes, value, false, this.transcoder), timeout);
	}

	@Override
	public void appendWithNoReply(String key, Object value)
			throws InterruptedException, MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		try {
			sendStoreCommand(this.commandFactory.createAppendCommand(key,
					keyBytes, value, true, this.transcoder), this.opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String,
	 * java.lang.Object)
	 */
	public final boolean prepend(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return prepend(key, value, this.opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String,
	 * java.lang.Object, long)
	 */
	public final boolean prepend(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		return sendStoreCommand(this.commandFactory.createPrependCommand(key,
				keyBytes, value, false, this.transcoder), timeout);
	}

	@Override
	public void prependWithNoReply(String key, Object value)
			throws InterruptedException, MemcachedException {
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		try {
			sendStoreCommand(this.commandFactory.createPrependCommand(key,
					keyBytes, value, true, this.transcoder), this.opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
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
		return cas(key, exp, value, this.opTimeout, cas);
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
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		return sendStoreCommand(this.commandFactory.createCASCommand(key,
				keyBytes, exp, value, cas, false, transcoder), timeout);
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
		return cas(key, exp, value, transcoder, this.opTimeout, cas);
	}

	private final <T> boolean cas0(final String key, final int exp,
			GetsResponse<T> getsResponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder, byte[] keyBytes, boolean noreply)
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
				&& !sendStoreCommand(this.commandFactory.createCASCommand(key,
						keyBytes, exp, operation.getNewValue(result.getCas(),
								result.getValue()), result.getCas(), noreply,
						transcoder), this.opTimeout)) {
			tryCount++;
			result = gets0(key, keyBytes, transcoder);
			if (result == null) {
				throw new MemcachedException(
						"could not gets the value for Key=" + key);
			}
			if (tryCount > operation.getMaxTries()) {
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
		return cas0(key, exp, result, operation, transcoder, keyBytes, false);
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
		return cas0(key, exp, getsReponse, operation, transcoder, keyBytes,
				false);
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

	@Override
	public <T> void casWithNoReply(String key, CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException {
		casWithNoReply(key, 0, operation);
	}

	@Override
	public <T> void casWithNoReply(String key, GetsResponse<T> getsResponse,
			CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		casWithNoReply(key, 0, getsResponse, operation);

	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void casWithNoReply(String key, int exp,
			CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		GetsResponse<T> result = gets0(key, keyBytes, this.transcoder);
		casWithNoReply(key, exp, result, operation);

	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void casWithNoReply(String key, int exp,
			GetsResponse<T> getsReponse, CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		cas0(key, exp, getsReponse, operation, this.transcoder, keyBytes, true);

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
		return delete0(key, time, false);
	}

	/**
	 * Delete key's data item from memcached.This method doesn't wait for reply
	 * 
	 * @param key
	 * @param time
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final void deleteWithNoReply(final String key, final int time)
			throws InterruptedException, MemcachedException {
		try {
			delete0(key, time, true);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public final void deleteWithNoReply(final String key)
			throws InterruptedException, MemcachedException {
		deleteWithNoReply(key, 0);
	}

	private boolean delete0(final String key, final int time, boolean noreply)
			throws MemcachedException, InterruptedException, TimeoutException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = this.commandFactory.createDeleteCommand(key,
				keyBytes, time, noreply);
		sendCommand(command);
		if (!command.isNoreply()) {
			latchWait(command, this.opTimeout);
			command.getIoBuffer().free();
			checkException(command);
			if (command.getResult() == null) {
				throw new MemcachedException(
						"Operation fail,may be caused by networking or timeout");
			}
		} else {
			return false;
		}
		return (Boolean) command.getResult();
	}

	void checkException(final Command command) throws MemcachedException {
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
		final Command command = this.commandFactory.createVersionCommand(
				new CountDownLatch(1), null);
		sendCommand(command);
		latchWait(command, this.opTimeout);
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
		return sendIncrOrDecrCommand(key, num, CommandType.INCR, false);
	}

	public final void incrWithNoReply(final String key, final int num)
			throws InterruptedException, MemcachedException {
		try {
			sendIncrOrDecrCommand(key, num, CommandType.INCR, true);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public final void decrWithNoReply(final String key, final int num)
			throws InterruptedException, MemcachedException {
		try {
			sendIncrOrDecrCommand(key, num, CommandType.DECR, true);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#decr(java.lang.String, int)
	 */
	public final int decr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendIncrOrDecrCommand(key, num, CommandType.DECR, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll()
	 */
	public final void flushAll() throws TimeoutException, InterruptedException,
			MemcachedException {
		flushAll(this.opTimeout);
	}

	@Override
	public void flushAllWithNoReply() throws InterruptedException,
			MemcachedException {
		try {
			flushAllMemcachedServers(this.opTimeout, true);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	@Override
	public void flushAllWithNoReply(InetSocketAddress address)
			throws MemcachedException, InterruptedException {
		try {
			flushSpecialMemcachedServer(address, this.opTimeout, true);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(long)
	 */
	public final void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		flushAllMemcachedServers(timeout, false);
	}

	private void flushAllMemcachedServers(long timeout, boolean noreply)
			throws MemcachedException, InterruptedException, TimeoutException {
		final Collection<Session> sessions = this.connector.getSessionSet();
		CountDownLatch latch = new CountDownLatch(sessions.size());
		List<Command> commands = new ArrayList<Command>(sessions.size());
		for (Session session : sessions) {
			if (session != null && !session.isClosed()) {
				Command command = this.commandFactory.createFlushAllCommand(
						latch, 0, noreply);

				session.send(command);
			} else {
				latch.countDown();
			}
		}
		if (!noreply) {
			if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
				for (Command cmd : commands) {
					cmd.cancel();
				}
				throw new TimeoutException("Timed out waiting for operation");
			}
		}
	}

	@Override
	public void setLoggingLevelVerbosity(InetSocketAddress address, int level)
			throws TimeoutException, InterruptedException, MemcachedException {
		setMemcachedLoggingLevel(address, level, false);

	}

	private void setMemcachedLoggingLevel(InetSocketAddress address, int level,
			boolean noreply) throws MemcachedException, InterruptedException,
			TimeoutException {
		if (address == null) {
			throw new IllegalArgumentException("Null adderss");
		}
		CountDownLatch latch = new CountDownLatch(1);

		Session session = this.connector.getSessionByAddress(address);
		if (session == null) {
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		}
		Command command = this.commandFactory.createVerbosityCommand(latch,
				level, noreply);
		session.send(command);
		if (!noreply) {
			latchWait(command, this.opTimeout);
		}
	}

	@Override
	public void setLoggingLevelVerbosityWithNoReply(InetSocketAddress address,
			int level) throws InterruptedException, MemcachedException {
		try {
			setMemcachedLoggingLevel(address, level, true);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.lang.String,
	 * long)
	 */
	@Deprecated
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
		flushAll(address, this.opTimeout);
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
		flushSpecialMemcachedServer(address, timeout, false);
	}

	private void flushSpecialMemcachedServer(InetSocketAddress address,
			long timeout, boolean noreply) throws MemcachedException,
			InterruptedException, TimeoutException {
		if (address == null) {
			throw new IllegalArgumentException("Null adderss");
		}
		CountDownLatch latch = new CountDownLatch(1);

		Session session = this.connector.getSessionByAddress(address);
		if (session == null) {
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		}
		Command command = this.commandFactory.createFlushAllCommand(latch, 0,
				noreply);
		session.send(command);
		if (!noreply) {
			latchWait(command, timeout);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.lang.String)
	 */
	public final void flushAll(String host) throws TimeoutException,
			InterruptedException, MemcachedException {
		flushAll(host, this.opTimeout);
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
		return stats(host, this.opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#stats(java.net.InetSocketAddress)
	 */
	public final Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException {
		return stats(address, this.opTimeout);
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
		if (address == null) {
			throw new IllegalArgumentException("Null inetSocketAddress");
		}
		CountDownLatch latch = new CountDownLatch(1);

		Session session = this.connector.getSessionByAddress(address);
		if (session == null) {
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		}
		Command command = this.commandFactory.createStatsCommand(address,
				latch, null);
		session.send(command);
		latchWait(command, timeout);
		return (Map<String, String>) command.getResult();
	}

	@Override
	public final Map<InetSocketAddress, Map<String, String>> getStats()
			throws MemcachedException, InterruptedException, TimeoutException {
		return getStats(this.opTimeout);
	}

	@Override
	public final Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName) throws MemcachedException, InterruptedException,
			TimeoutException {
		return getStatsByItem(itemName, this.opTimeout);
	}

	@SuppressWarnings("unchecked")
	public final Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName, long timeout) throws MemcachedException,
			InterruptedException, TimeoutException {
		final Set<Session> sessionSet = this.connector.getSessionSet();
		final Map<InetSocketAddress, Map<String, String>> collectResult = new HashMap<InetSocketAddress, Map<String, String>>();
		if (sessionSet.size() == 0) {
			return collectResult;
		}
		final CountDownLatch latch = new CountDownLatch(sessionSet.size());
		List<Command> commands = new ArrayList<Command>(sessionSet.size());
		for (Session session : sessionSet) {
			Command command = this.commandFactory.createStatsCommand(session
					.getRemoteSocketAddress(), latch, itemName);

			session.send(command);
			commands.add(command);

		}
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			for (Command command : commands) {
				command.cancel();
			}
			throw new TimeoutException("Timed out waiting for operation");
		}
		for (Command command : commands) {
			checkException(command);
			collectResult.put(((StatsCommand) command).getServer(),
					(Map<String, String>) command.getResult());
		}
		return collectResult;
	}

	@Override
	public final Map<InetSocketAddress, String> getVersions()
			throws TimeoutException, InterruptedException, MemcachedException {
		return getVersions(this.opTimeout);
	}

	public final Map<InetSocketAddress, String> getVersions(long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		final Set<Session> sessionSet = this.connector.getSessionSet();
		Map<InetSocketAddress, String> collectResult = new HashMap<InetSocketAddress, String>();
		if (sessionSet.size() == 0) {
			return collectResult;
		}
		final CountDownLatch latch = new CountDownLatch(sessionSet.size());
		List<Command> commands = new ArrayList<Command>(sessionSet.size());
		for (Session session : sessionSet) {
			Command command = this.commandFactory.createVersionCommand(latch,
					session.getRemoteSocketAddress());
			session.send(command);
			commands.add(command);

		}

		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			for (Command command : commands) {
				command.cancel();
			}
			throw new TimeoutException("Timed out waiting for operation");
		}
		for (Command command : commands) {
			checkException(command);
			collectResult.put(((VersionCommand) command).getServer(),
					(String) command.getResult());
		}
		return collectResult;
	}

	@Override
	public Map<InetSocketAddress, Map<String, String>> getStats(long timeout)
			throws MemcachedException, InterruptedException, TimeoutException {
		return getStatsByItem(null, timeout);
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

	private int sendIncrOrDecrCommand(final String key, final int num,
			final CommandType cmdType, boolean noreply)
			throws InterruptedException, TimeoutException, MemcachedException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = this.commandFactory.createIncrDecrCommand(key,
				keyBytes, num, cmdType, noreply);
		sendCommand(command);
		if (!command.isNoreply()) {
			latchWait(command, this.opTimeout);
			command.getIoBuffer().free();
			checkException(command);
			if (command.getResult() == null) {
				throw new MemcachedException(
						"Operation fail,may be caused by networking or timeout");
			}
			return (Integer) command.getResult();
		} else {
			return -1;
		}
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
		return this.transcoder;
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

	private final <T> boolean sendStoreCommand(Command command, long timeout)
			throws InterruptedException, TimeoutException, MemcachedException {

		sendCommand(command);
		if (!command.isNoreply()) {
			latchWait(command, timeout);
			command.getIoBuffer().free();
			checkException(command);
			if (command.getResult() == null) {
				throw new MemcachedException(
						"Operation fail,may be caused by networking or timeout");
			}
		} else {
			return false;
		}
		return (Boolean) command.getResult();
	}

	private void latchWait(final Command cmd, final long timeout)
			throws InterruptedException, TimeoutException {
		if (!cmd.getLatch().await(timeout, TimeUnit.MILLISECONDS)) {
			cmd.cancel();
			throw new TimeoutException("Timed out(" + timeout
					+ ") waiting for operation");
		}
	}

	@Override
	public final Collection<InetSocketAddress> getAvaliableServers() {
		Set<Session> sessionSet = this.connector.getSessionSet();
		Set<InetSocketAddress> result = new HashSet<InetSocketAddress>();
		for (Session session : sessionSet) {
			result.add(session.getRemoteSocketAddress());
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public void addStateListener(MemcachedClientStateListener listener) {
		MemcachedClientStateListenerAdapter adapter = new MemcachedClientStateListenerAdapter(
				listener, this);
		this.stateListenerAdapters.add(adapter);
		this.connector.addStateListener(adapter);
	}

	@Override
	public Collection<MemcachedClientStateListener> getStateListeners() {
		final List<MemcachedClientStateListener> result = new ArrayList<MemcachedClientStateListener>(
				this.stateListenerAdapters.size());
		for (MemcachedClientStateListenerAdapter adapter : this.stateListenerAdapters) {
			result.add(adapter.getMemcachedClientStateListener());
		}
		return result;
	}

	@Override
	public void removeStateListener(MemcachedClientStateListener listener) {
		for (MemcachedClientStateListenerAdapter adapter : this.stateListenerAdapters) {
			if (adapter.getMemcachedClientStateListener().equals(listener)) {
				this.stateListenerAdapters.remove(adapter);
				this.connector.removeStateListener(adapter);
			}
		}
	}
}
