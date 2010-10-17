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
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.KeyIteratorImpl;
import net.rubyeye.xmemcached.impl.MemcachedClientStateListenerAdapter;
import net.rubyeye.xmemcached.impl.MemcachedConnector;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.monitor.MemcachedClientNameHolder;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.networking.Connector;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;
import net.rubyeye.xmemcached.utils.Protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;
import com.google.code.yanf4j.util.SystemUtils;

/**
 * Memcached Client for connecting to memcached server and do operations.
 * 
 * @author dennis(killme2008@gmail.com)
 * 
 */
public class XMemcachedClient implements XMemcachedClientMBean, MemcachedClient {

	private static final Logger log = LoggerFactory
			.getLogger(XMemcachedClient.class);
	protected MemcachedSessionLocator sessionLocator;
	private volatile boolean shutdown;
	protected Connector connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	private boolean sanitizeKeys;
	private MemcachedHandler memcachedHandler;
	protected CommandFactory commandFactory;
	private long opTimeout = DEFAULT_OP_TIMEOUT;
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT; // 连接超时
	protected int connectionPoolSize = DEFAULT_CONNECTION_POOL_SIZE;

	protected final AtomicInteger serverOrderCount = new AtomicInteger();

	private Map<InetSocketAddress, AuthInfo> authInfoMap = new HashMap<InetSocketAddress, AuthInfo>();

	private String name; // cache name

	private final CopyOnWriteArrayList<MemcachedClientStateListenerAdapter> stateListenerAdapters = new CopyOnWriteArrayList<MemcachedClientStateListenerAdapter>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#setMergeFactor(int)
	 */
	public final void setMergeFactor(final int mergeFactor) {
		if (mergeFactor < 0) {
			throw new IllegalArgumentException("mergeFactor<0");
		}
		connector.setMergeFactor(mergeFactor);
	}

	public final MemcachedSessionLocator getSessionLocator() {
		return sessionLocator;
	}

	public final CommandFactory getCommandFactory() {
		return commandFactory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (!isShutdown())
			throw new IllegalStateException(
					"Could not set name when xmc has been started");
		this.name = name;
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
		if (connectTimeout < 0) {
			throw new IllegalArgumentException("connectTimeout<0");
		}
		this.connectTimeout = connectTimeout;
	}

	public void setEnableHeartBeat(boolean enableHeartBeat) {
		memcachedHandler.setEnableHeartBeat(enableHeartBeat);
	}

	/**
	 * get operation timeout setting
	 * 
	 * @return
	 */
	public final long getOpTimeout() {
		return opTimeout;
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

	public void setHealSessionInterval(long healConnectionInterval) {
		if (null != connector) {
			connector.setHealSessionInterval(healConnectionInterval);
		}

	}

	public long getHealSessionInterval() {
		if (null != connector) {
			return connector.getHealSessionInterval();
		}
		return -1L;
	}

	public Map<InetSocketAddress, AuthInfo> getAuthInfoMap() {
		return authInfoMap;
	}

	public void setAuthInfoMap(Map<InetSocketAddress, AuthInfo> map) {
		authInfoMap = map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getConnector()
	 */
	public final Connector getConnector() {
		return connector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#setOptimizeMergeBuffer(boolean)
	 */
	public final void setOptimizeMergeBuffer(final boolean optimizeMergeBuffer) {
		connector.setOptimizeMergeBuffer(optimizeMergeBuffer);
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
				CommandType.GETS_ONE, opTimeout, transcoder);
		return result;
	}

	private final void sendCommand(final Command cmd) throws MemcachedException {
		if (shutdown) {
			throw new MemcachedException("Xmemcached is stopped");
		}
		connector.send(cmd);
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
				new SimpleBufferAllocator(), XMemcachedClientBuilder
						.getDefaultConfiguration(), XMemcachedClientBuilder
						.getDefaultSocketOptions(), new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
		connect(new InetSocketAddressWrapper(newSocketAddress(server, port),
				serverOrderCount.incrementAndGet()), weight);
	}

	protected InetSocketAddress newSocketAddress(final String server,
			final int port) {
		return new InetSocketAddress(server, port);
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
		connect(new InetSocketAddressWrapper(newSocketAddress(server, port),
				serverOrderCount.incrementAndGet()), weight);
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
		connect(new InetSocketAddressWrapper(inetSocketAddress,
				serverOrderCount.incrementAndGet()), weight);
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
				connect(new InetSocketAddressWrapper(address, serverOrderCount
						.incrementAndGet()), 1);
			}
		}
	}

	public void addOneServerWithWeight(String server, int weight)
			throws IOException {
		InetSocketAddress address = AddrUtil.getOneAddress(server);
		if (address == null) {
			throw new IllegalArgumentException("Null Server");
		}
		if (weight <= 0) {
			throw new IllegalArgumentException("weight<=0");
		}
		connect(new InetSocketAddressWrapper(address, serverOrderCount
				.incrementAndGet()), weight);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#getServersDescription()
	 */
	public final List<String> getServersDescription() {
		final List<String> result = new ArrayList<String>();
		for (Session session : connector.getSessionSet()) {
			InetSocketAddress socketAddress = session.getRemoteSocketAddress();
			int weight = ((MemcachedSession) session).getWeight();
			result.add(socketAddress.getHostName() + ":"
					+ socketAddress.getPort() + "(weight=" + weight + ")");
		}
		return result;
	}

	public final void setServerWeight(String server, int weight) {
		InetSocketAddress socketAddress = AddrUtil.getOneAddress(server);
		Queue<Session> sessionQueue = connector
				.getSessionByAddress(socketAddress);
		if (sessionQueue == null) {
			throw new IllegalArgumentException("There is no server " + server);
		}
		for (Session session : sessionQueue) {
			if (session != null) {
				((MemcachedTCPSession) session).setWeight(weight);
			}
		}
		connector.updateSessions();
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
				Queue<Session> sessionQueue = connector
						.getSessionByAddress(address);
				if (sessionQueue != null) {
					for (Session session : sessionQueue) {
						if (session != null) {
							// Disable auto reconnection
							((MemcachedSession) session)
									.setAllowReconnect(false);
							// Close connection
							((MemcachedSession) session).quit();
						}
					}
				}
				connector.removeReconnectRequest(address);
			}

		}

	}

	protected void checkSocketAddress(InetSocketAddress address) {

	}

	private void connect(
			final InetSocketAddressWrapper inetSocketAddressWrapper, int weight)
			throws IOException {
		// creat connection pool
		InetSocketAddress inetSocketAddress = inetSocketAddressWrapper
				.getInetSocketAddress();
		checkSocketAddress(inetSocketAddress);
		for (int i = 0; i < connectionPoolSize; i++) {
			Future<Boolean> future = null;
			boolean connected = false;
			Throwable throwable = null;
			try {
				future = connector.connect(inetSocketAddressWrapper, weight);

				if (!future.get(connectTimeout, TimeUnit.MILLISECONDS)) {
					log.error("connect to " + inetSocketAddress.getHostName()
							+ ":" + inetSocketAddress.getPort() + " fail");
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
				connector.addToWatingQueue(new ReconnectRequest(
						inetSocketAddressWrapper, 0, weight,
						getHealSessionInterval()));
				log.error("Connect to " + inetSocketAddress.getHostName() + ":"
						+ inetSocketAddress.getPort() + " fail", throwable);
				// throw new IOException(throwable);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private final <T> Object fetch0(final String key, final byte[] keyBytes,
			final CommandType cmdType, final long timeout,
			Transcoder<T> transcoder) throws InterruptedException,
			TimeoutException, MemcachedException, MemcachedException {
		final Command command = commandFactory.createGetCommand(key, keyBytes,
				cmdType, this.transcoder);
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
		MemcachedClientNameHolder.clear();
	}

	private final void startConnector() throws IOException {
		if (shutdown) {
			shutdown = false;
			connector.start();
			memcachedHandler.start();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						XMemcachedClient.this.shutdown();
					} catch (IOException e) {
						log.error("Shutdown XMemcachedClient error", e);
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void buildConnector(MemcachedSessionLocator locator,
			BufferAllocator bufferAllocator, Configuration configuration,
			Map<SocketOption, Object> socketOptions,
			CommandFactory commandFactory, Transcoder transcoder) {
		if (locator == null) {
			locator = new ArrayMemcachedSessionLocator();

		}
		if (bufferAllocator == null) {
			bufferAllocator = new SimpleBufferAllocator();
		}
		if (configuration == null) {
			configuration = XMemcachedClientBuilder.getDefaultConfiguration();
		}
		if (transcoder == null) {
			transcoder = new SerializingTranscoder();
		}
		if (commandFactory == null) {
			commandFactory = new TextCommandFactory();
		}
		if (name == null) {
			name = "MemcachedClient-"
					+ Constants.MEMCACHED_CLIENT_COUNTER.getAndIncrement();
			MemcachedClientNameHolder.setName(name);
		}
		this.commandFactory = commandFactory;
		ByteUtils.setProtocol(this.commandFactory.getProtocol());
		log.warn("XMemcachedClient use "
				+ this.commandFactory.getProtocol().name() + " protocol");
		this.commandFactory.setBufferAllocator(bufferAllocator);
		shutdown = true;
		this.transcoder = transcoder;
		sessionLocator = locator;
		connector = newConnector(bufferAllocator, configuration,
				sessionLocator, this.commandFactory, connectionPoolSize);
		memcachedHandler = new MemcachedHandler(this);
		connector.setHandler(memcachedHandler);
		connector.setCodecFactory(new MemcachedCodecFactory());
		connector.setSessionTimeout(-1);
		connector.setSocketOptions(socketOptions);
	}

	protected Connector newConnector(BufferAllocator bufferAllocator,
			Configuration configuration,
			MemcachedSessionLocator memcachedSessionLocator,
			CommandFactory commandFactory, int i) {
		// make sure dispatch message thread count is zero
		configuration.setDispatchMessageThreadCount(0);
		return new MemcachedConnector(configuration, memcachedSessionLocator,
				bufferAllocator, commandFactory, i);
	}

	private final void registerMBean() {
		if (shutdown) {
			XMemcachedMbeanServer.getInstance().registMBean(
					this,
					this.getClass().getPackage().getName() + ":type="
							+ this.getClass().getSimpleName() + "-"
							+ MemcachedClientNameHolder.getName());
		}
	}

	public void setOptimizeGet(boolean optimizeGet) {
		connector.setOptimizeGet(optimizeGet);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#setBufferAllocator(net.rubyeye
	 * .xmemcached.buffer.BufferAllocator)
	 */
	public final void setBufferAllocator(final BufferAllocator bufferAllocator) {
		connector.setBufferAllocator(bufferAllocator);
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
				new SimpleBufferAllocator(), XMemcachedClientBuilder
						.getDefaultConfiguration(), XMemcachedClientBuilder
						.getDefaultSocketOptions(), new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
		connect(new InetSocketAddressWrapper(inetSocketAddress,
				serverOrderCount.incrementAndGet()), weight);
	}

	public XMemcachedClient(final InetSocketAddress inetSocketAddress)
			throws IOException {
		this(inetSocketAddress, 1);
	}

	public XMemcachedClient() throws IOException {
		super();
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), XMemcachedClientBuilder
						.getDefaultConfiguration(), XMemcachedClientBuilder
						.getDefaultSocketOptions(), new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
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
	XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			Map<SocketOption, Object> socketOptions,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList,
			List<MemcachedClientStateListener> stateListeners,
			Map<InetSocketAddress, AuthInfo> map, int poolSize, String name)
			throws IOException {
		super();
		setName(name);
		optimiezeSetReadThreadCount(conf, addressList);
		buildConnector(locator, allocator, conf, socketOptions, commandFactory,
				transcoder);
		if (stateListeners != null) {
			for (MemcachedClientStateListener stateListener : stateListeners) {
				addStateListener(stateListener);
			}
		}
		setAuthInfoMap(map);
		setConnectionPoolSize(poolSize);
		start0();
		if (addressList != null) {
			for (InetSocketAddress inetSocketAddress : addressList) {
				connect(new InetSocketAddressWrapper(inetSocketAddress,
						serverOrderCount.incrementAndGet()), 1);
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
	 * @param stateListeners
	 *            weight array for address list
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			Map<SocketOption, Object> socketOptions,
			CommandFactory commandFactory, Transcoder transcoder,
			List<InetSocketAddress> addressList, int[] weights,
			List<MemcachedClientStateListener> stateListeners,
			Map<InetSocketAddress, AuthInfo> infoMap, int poolSize,final String name)
			throws IOException {
		super();
		setName(name);
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
		buildConnector(locator, allocator, conf, socketOptions, commandFactory,
				transcoder);
		if (stateListeners != null) {
			for (MemcachedClientStateListener stateListener : stateListeners) {
				addStateListener(stateListener);
			}
		}
		setAuthInfoMap(infoMap);
		setConnectionPoolSize(poolSize);
		start0();
		if (addressList != null && weights != null) {
			for (int i = 0; i < addressList.size(); i++) {
				connect(new InetSocketAddressWrapper(addressList.get(i),
						serverOrderCount.incrementAndGet()), weights[i]);
			}
		}
	}

	private final void optimiezeSetReadThreadCount(Configuration conf,
			List<InetSocketAddress> addressList) {
		if (conf != null && addressList != null) {
			if (isLinuxPlatform() && addressList.size() > 1
					&& conf.getReadThreadCount() == DEFAULT_READ_THREAD_COUNT) {
				int threadCount = 2 * SystemUtils.getSystemThreadCount();
				conf
						.setReadThreadCount(addressList.size() > threadCount ? threadCount
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
				simpleBufferAllocator, XMemcachedClientBuilder
						.getDefaultConfiguration(), XMemcachedClientBuilder
						.getDefaultSocketOptions(), new TextCommandFactory(),
				new SerializingTranscoder());
		start0();
		for (InetSocketAddress inetSocketAddress : addressList) {
			connect(new InetSocketAddressWrapper(inetSocketAddress,
					serverOrderCount.incrementAndGet()), 1);

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
		return (T) get(key, timeout, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String,
	 * net.rubyeye.xmemcached.transcoders.Transcoder)
	 */
	public final <T> T get(final String key, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(key, opTimeout, transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (T) get(key, opTimeout);
	}

	private <T> Object get0(String key, final long timeout,
			final CommandType cmdType, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
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
		return gets(key, opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String, long)
	 */
	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(key, timeout, transcoder);
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
		return gets(key, opTimeout, transcoder);
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
		return getMulti0(keyCollections, opTimeout, CommandType.GET_MANY,
				transcoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection)
	 */
	public final <T> Map<String, T> get(final Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(keyCollections, opTimeout);
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
		return get(keyCollections, timeout, transcoder);
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
		return gets(keyCollections, opTimeout);
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
		return gets(keyCollections, timeout, transcoder);
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
		return gets(keyCollections, opTimeout, transcoder);
	}

	private final <T> Map<String, T> getMulti0(final Collection<String> keys,
			final long timeout, final CommandType cmdType,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		if (keys == null || keys.size() == 0) {
			return null;
		}
		Collection<String> keyCollections = keys;
		if (sanitizeKeys) {
			keyCollections = new ArrayList<String>(keys.size());
			for (String key : keys) {
				keyCollections.add(sanitizeKey(key));
			}
		}
		final CountDownLatch latch;
		final List<Command> commands;
		if (connector.getSessionSet().size() <= 1) {
			commands = new ArrayList<Command>(1);
			latch = new CountDownLatch(1);
			commands.add(sendGetMultiCommand(keyCollections, latch, cmdType,
					transcoder));

		} else {
			Collection<List<String>> catalogKeys = catalogKeys(keyCollections);
			commands = new ArrayList<Command>(catalogKeys.size());
			latch = new CountDownLatch(catalogKeys.size());
			for (List<String> catalogKeyCollection : catalogKeys) {
				commands.add(sendGetMultiCommand(catalogKeyCollection, latch,
						cmdType, transcoder));
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
					if (sanitizeKeys) {
						result.put(decodeKey(entry.getKey()), transcoder
								.decode(entry.getValue()));
					} else {
						result.put(entry.getKey(), transcoder.decode(entry
								.getValue()));
					}
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
			Session index = sessionLocator.getSessionByKey(key);
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
		sendCommand(command);
		return command;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long)
	 */
	public final <T> boolean set(String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		return sendStoreCommand(commandFactory.createSetCommand(key, keyBytes,
				exp, value, false, transcoder), timeout);
	}

	@SuppressWarnings("unchecked")
	public void setWithNoReply(String key, int exp, Object value)
			throws InterruptedException, MemcachedException {
		setWithNoReply(key, exp, value, transcoder);
	}

	public <T> void setWithNoReply(String key, int exp, T value,
			Transcoder<T> transcoder) throws InterruptedException,
			MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		try {
			sendStoreCommand(commandFactory.createSetCommand(key, keyBytes,
					exp, value, true, transcoder), opTimeout);
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
		return set(key, exp, value, opTimeout);
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
		return set(key, exp, value, transcoder, timeout);
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
		return set(key, exp, value, transcoder, opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long)
	 */
	public final <T> boolean add(String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		return sendStoreCommand(commandFactory.createAddCommand(key, keyBytes,
				exp, value, false, transcoder), timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int,
	 * java.lang.Object)
	 */
	public final boolean add(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, opTimeout);
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
		return add(key, exp, value, transcoder, timeout);
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
		return add(key, exp, value, transcoder, opTimeout);
	}

	@SuppressWarnings("unchecked")
	public void addWithNoReply(String key, int exp, Object value)
			throws InterruptedException, MemcachedException {
		addWithNoReply(key, exp, value, transcoder);

	}

	public <T> void addWithNoReply(String key, int exp, T value,
			Transcoder<T> transcoder) throws InterruptedException,
			MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		try {
			sendStoreCommand(commandFactory.createAddCommand(key, keyBytes,
					exp, value, true, transcoder), opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}

	}

	@SuppressWarnings("unchecked")
	public void replaceWithNoReply(String key, int exp, Object value)
			throws InterruptedException, MemcachedException {
		replaceWithNoReply(key, exp, value, transcoder);

	}

	public <T> void replaceWithNoReply(String key, int exp, T value,
			Transcoder<T> transcoder) throws InterruptedException,
			MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		try {
			sendStoreCommand(commandFactory.createReplaceCommand(key, keyBytes,
					exp, value, true, transcoder), opTimeout);
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
	public final <T> boolean replace(String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, exp, value);
		return sendStoreCommand(commandFactory.createReplaceCommand(key,
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
		return replace(key, exp, value, opTimeout);
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
		return replace(key, exp, value, transcoder, timeout);
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
		return replace(key, exp, value, transcoder, opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String,
	 * java.lang.Object)
	 */
	public final boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return append(key, value, opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String,
	 * java.lang.Object, long)
	 */
	public final boolean append(String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		return sendStoreCommand(commandFactory.createAppendCommand(key,
				keyBytes, value, false, transcoder), timeout);
	}

	public void appendWithNoReply(String key, Object value)
			throws InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		try {
			sendStoreCommand(commandFactory.createAppendCommand(key, keyBytes,
					value, true, transcoder), opTimeout);
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
		return prepend(key, value, opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String,
	 * java.lang.Object, long)
	 */
	public final boolean prepend(String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		return sendStoreCommand(commandFactory.createPrependCommand(key,
				keyBytes, value, false, transcoder), timeout);
	}

	public void prependWithNoReply(String key, Object value)
			throws InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		try {
			sendStoreCommand(commandFactory.createPrependCommand(key, keyBytes,
					value, true, transcoder), opTimeout);
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
		return cas(key, exp, value, opTimeout, cas);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, T,
	 * net.rubyeye.xmemcached.transcoders.Transcoder, long, long)
	 */
	public final <T> boolean cas(String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = checkStoreArguments(key, 0, value);
		return sendStoreCommand(commandFactory.createCASCommand(key, keyBytes,
				exp, value, cas, false, transcoder), timeout);
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
		return cas(key, exp, value, transcoder, timeout, cas);
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
		return cas(key, exp, value, transcoder, opTimeout, cas);
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
		while (tryCount <= operation.getMaxTries()
				&& result != null
				&& !sendStoreCommand(commandFactory.createCASCommand(key,
						keyBytes, exp, operation.getNewValue(result.getCas(),
								result.getValue()), result.getCas(), noreply,
						transcoder), opTimeout) && !noreply) {
			tryCount++;
			result = gets0(key, keyBytes, transcoder);
			if (result == null) {
				throw new MemcachedException(
						"could not gets the value for Key=" + key + " for cas");
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
	public final <T> boolean cas(String key, final int exp,
			final CASOperation<T> operation, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
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
	public final <T> boolean cas(String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
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

		return cas(key, exp, getsReponse, operation, transcoder);
	}

	public <T> void casWithNoReply(String key, CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException {
		casWithNoReply(key, 0, operation);
	}

	public <T> void casWithNoReply(String key, GetsResponse<T> getsResponse,
			CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		casWithNoReply(key, 0, getsResponse, operation);

	}

	@SuppressWarnings("unchecked")
	public <T> void casWithNoReply(String key, int exp,
			CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = ByteUtils.getBytes(key);
		GetsResponse<T> result = gets0(key, keyBytes, transcoder);
		casWithNoReply(key, exp, result, operation);

	}

	@SuppressWarnings("unchecked")
	public <T> void casWithNoReply(String key, int exp,
			GetsResponse<T> getsReponse, CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		cas0(key, exp, getsReponse, operation, transcoder, keyBytes, true);

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
		return cas(key, exp, operation, transcoder);
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

	private boolean delete0(String key, final int time, boolean noreply)
			throws MemcachedException, InterruptedException, TimeoutException {
		key = sanitizeKey(key);
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = commandFactory.createDeleteCommand(key,
				keyBytes, time, noreply);
		sendCommand(command);
		if (!command.isNoreply()) {
			latchWait(command, opTimeout);
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
	 * @see net.rubyeye.xmemcached.MemcachedClient#incr(java.lang.String, int)
	 */
	public final long incr(String key, final long delta)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		return sendIncrOrDecrCommand(key, delta, 0, CommandType.INCR, false,
				opTimeout);
	}

	public long incr(String key, long delta, long initValue)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		return sendIncrOrDecrCommand(key, delta, initValue, CommandType.INCR,
				false, opTimeout);
	}

	public long incr(String key, long delta, long initValue, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		return sendIncrOrDecrCommand(key, delta, initValue, CommandType.INCR,
				false, timeout);
	}

	public final void incrWithNoReply(String key, long delta)
			throws InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		try {
			sendIncrOrDecrCommand(key, delta, 0, CommandType.INCR, true,
					opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public final void decrWithNoReply(String key, final long delta)
			throws InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		try {
			sendIncrOrDecrCommand(key, delta, 0, CommandType.DECR, true,
					opTimeout);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#decr(java.lang.String, int)
	 */
	public final long decr(String key, final long delta)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		return sendIncrOrDecrCommand(key, delta, 0, CommandType.DECR, false,
				opTimeout);
	}

	public long decr(String key, long delta, long initValue)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		return sendIncrOrDecrCommand(key, delta, initValue, CommandType.DECR,
				false, opTimeout);
	}

	public long decr(String key, long delta, long initValue, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		key = sanitizeKey(key);
		return sendIncrOrDecrCommand(key, delta, initValue, CommandType.DECR,
				false, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll()
	 */
	public final void flushAll() throws TimeoutException, InterruptedException,
			MemcachedException {
		flushAll(opTimeout);
	}

	public void flushAllWithNoReply() throws InterruptedException,
			MemcachedException {
		try {
			flushAllMemcachedServers(opTimeout, true, 0);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public void flushAllWithNoReply(int exptime) throws InterruptedException,
			MemcachedException {
		try {
			flushAllMemcachedServers(opTimeout, true, exptime);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public void flushAllWithNoReply(InetSocketAddress address)
			throws MemcachedException, InterruptedException {
		try {
			flushSpecialMemcachedServer(address, opTimeout, true, 0);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public void flushAllWithNoReply(InetSocketAddress address, int exptime)
			throws MemcachedException, InterruptedException {
		try {
			flushSpecialMemcachedServer(address, opTimeout, true, exptime);
		} catch (TimeoutException e) {
			throw new MemcachedException(e);
		}
	}

	public final void flushAll(int exptime, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		flushAllMemcachedServers(timeout, false, exptime);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(long)
	 */
	public final void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		flushAllMemcachedServers(timeout, false, 0);
	}

	private void flushAllMemcachedServers(long timeout, boolean noreply,
			int exptime) throws MemcachedException, InterruptedException,
			TimeoutException {
		final Collection<Session> sessions = connector.getSessionSet();
		CountDownLatch latch = new CountDownLatch(sessions.size());
		List<Command> commands = new ArrayList<Command>(sessions.size());
		for (Session session : sessions) {
			if (session != null && !session.isClosed()) {
				Command command = commandFactory.createFlushAllCommand(latch,
						exptime, noreply);

				session.write(command);
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

		Queue<Session> sessionQueue = connector.getSessionByAddress(address);
		if (sessionQueue == null || sessionQueue.peek() == null) {
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		}

		Command command = commandFactory.createVerbosityCommand(latch, level,
				noreply);
		sessionQueue.peek().write(command);
		if (!noreply) {
			latchWait(command, opTimeout);
		}
	}

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
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#flushAll(java.net.InetSocketAddress
	 * )
	 */
	public final void flushAll(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException {
		flushAll(address, opTimeout);
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
		flushSpecialMemcachedServer(address, timeout, false, 0);
	}

	public final void flushAll(InetSocketAddress address, long timeout,
			int exptime) throws MemcachedException, InterruptedException,
			TimeoutException {
		flushSpecialMemcachedServer(address, timeout, false, exptime);
	}

	private void flushSpecialMemcachedServer(InetSocketAddress address,
			long timeout, boolean noreply, int exptime)
			throws MemcachedException, InterruptedException, TimeoutException {
		if (address == null) {
			throw new IllegalArgumentException("Null adderss");
		}
		CountDownLatch latch = new CountDownLatch(1);

		Queue<Session> sessionQueue = connector.getSessionByAddress(address);
		if (sessionQueue == null || sessionQueue.peek() == null) {
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		}
		Command command = commandFactory.createFlushAllCommand(latch, exptime,
				noreply);
		sessionQueue.peek().write(command);
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
		flushAll(AddrUtil.getOneAddress(host), opTimeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClient#stats(java.net.InetSocketAddress)
	 */
	public final Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException {
		return stats(address, opTimeout);
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

		Queue<Session> sessionQueue = connector.getSessionByAddress(address);
		if (sessionQueue == null || sessionQueue.peek() == null) {
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		}
		Command command = commandFactory.createStatsCommand(address, latch,
				null);
		sessionQueue.peek().write(command);
		latchWait(command, timeout);
		return (Map<String, String>) command.getResult();
	}

	public final Map<InetSocketAddress, Map<String, String>> getStats()
			throws MemcachedException, InterruptedException, TimeoutException {
		return getStats(opTimeout);
	}

	public final Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName) throws MemcachedException, InterruptedException,
			TimeoutException {
		return getStatsByItem(itemName, opTimeout);
	}

	@SuppressWarnings("unchecked")
	public final Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName, long timeout) throws MemcachedException,
			InterruptedException, TimeoutException {
		final Set<Session> sessionSet = connector.getSessionSet();
		final Map<InetSocketAddress, Map<String, String>> collectResult = new HashMap<InetSocketAddress, Map<String, String>>();
		if (sessionSet.size() == 0) {
			return collectResult;
		}
		final CountDownLatch latch = new CountDownLatch(sessionSet.size());
		List<Command> commands = new ArrayList<Command>(sessionSet.size());
		for (Session session : sessionSet) {
			Command command = commandFactory.createStatsCommand(session
					.getRemoteSocketAddress(), latch, itemName);

			session.write(command);
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
			collectResult.put(((ServerAddressAware) command).getServer(),
					(Map<String, String>) command.getResult());
		}
		return collectResult;
	}

	public final Map<InetSocketAddress, String> getVersions()
			throws TimeoutException, InterruptedException, MemcachedException {
		return getVersions(opTimeout);
	}

	public final Map<InetSocketAddress, String> getVersions(long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		final Set<Session> sessionSet = connector.getSessionSet();
		Map<InetSocketAddress, String> collectResult = new HashMap<InetSocketAddress, String>();
		if (sessionSet.size() == 0) {
			return collectResult;
		}
		final CountDownLatch latch = new CountDownLatch(sessionSet.size());
		List<Command> commands = new ArrayList<Command>(sessionSet.size());
		for (Session session : sessionSet) {
			Command command = commandFactory.createVersionCommand(latch,
					session.getRemoteSocketAddress());
			session.write(command);
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
			collectResult.put(((ServerAddressAware) command).getServer(),
					(String) command.getResult());
		}
		return collectResult;
	}

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
		if (shutdown) {
			return;
		}
		shutdown = true;
		connector.quitAllSessions();
		connector.stop();
		memcachedHandler.stop();
		XMemcachedMbeanServer.getInstance().shutdown();
	}

	private long sendIncrOrDecrCommand(final String key, final long delta,
			long initValue, final CommandType cmdType, boolean noreply,
			long operationTimeout) throws InterruptedException,
			TimeoutException, MemcachedException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = commandFactory.createIncrDecrCommand(key,
				keyBytes, delta, initValue, 0, cmdType, noreply);
		sendCommand(command);
		if (!command.isNoreply()) {
			latchWait(command, operationTimeout);
			command.getIoBuffer().free();
			checkException(command);
			if (command.getResult() == null) {
				throw new MemcachedException(
						"Operation fail,may be caused by networking or timeout");
			}
			final Object result = command.getResult();
			if (result instanceof String) {
				if (((String) result).equals("NOT_FOUND")) {
					if (add(key, 0, String.valueOf(initValue), opTimeout)) {
						return initValue;
					} else {
						return sendIncrOrDecrCommand(key, delta, initValue,
								cmdType, noreply, operationTimeout);
					}
				} else {
					throw new MemcachedException(
							"Unknown result type for incr/decr:"
									+ result.getClass() + ",result=" + result);
				}
			} else {
				return (Long) command.getResult();
			}
		} else {
			return -1;
		}
	}

	public void setConnectionPoolSize(int poolSize) {
		if (!shutdown && getAvaliableServers().size() > 0) {
			throw new IllegalStateException(
					"Xmemcached client has been started");
		}
		if (poolSize <= 0) {
			throw new IllegalArgumentException("poolSize<=0");
		}
		connectionPoolSize = poolSize;
		connector.setConnectionPoolSize(poolSize);
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

	public final Collection<InetSocketAddress> getAvaliableServers() {
		Set<Session> sessionSet = connector.getSessionSet();
		Set<InetSocketAddress> result = new HashSet<InetSocketAddress>();
		for (Session session : sessionSet) {
			result.add(session.getRemoteSocketAddress());
		}
		return Collections.unmodifiableSet(result);
	}

	public final int getConnectionSizeBySocketAddress(InetSocketAddress address) {
		Queue<Session> sessionList = connector.getSessionByAddress(address);
		return sessionList == null ? 0 : sessionList.size();
	}

	public void addStateListener(MemcachedClientStateListener listener) {
		MemcachedClientStateListenerAdapter adapter = new MemcachedClientStateListenerAdapter(
				listener, this);
		stateListenerAdapters.add(adapter);
		connector.addStateListener(adapter);
	}

	public Collection<MemcachedClientStateListener> getStateListeners() {
		final List<MemcachedClientStateListener> result = new ArrayList<MemcachedClientStateListener>(
				stateListenerAdapters.size());
		for (MemcachedClientStateListenerAdapter adapter : stateListenerAdapters) {
			result.add(adapter.getMemcachedClientStateListener());
		}
		return result;
	}

	public void setPrimitiveAsString(boolean primitiveAsString) {
		transcoder.setPrimitiveAsString(primitiveAsString);
	}

	public void removeStateListener(MemcachedClientStateListener listener) {
		for (MemcachedClientStateListenerAdapter adapter : stateListenerAdapters) {
			if (adapter.getMemcachedClientStateListener().equals(listener)) {
				stateListenerAdapters.remove(adapter);
				connector.removeStateListener(adapter);
			}
		}
	}

	public Protocol getProtocol() {
		return commandFactory.getProtocol();
	}

	public boolean isSanitizeKeys() {
		return sanitizeKeys;
	}

	public void setSanitizeKeys(boolean sanitizeKeys) {
		this.sanitizeKeys = sanitizeKeys;
	}

	private String decodeKey(String key) throws MemcachedException {
		try {
			return sanitizeKeys ? URLDecoder.decode(key, "UTF-8") : key;
		} catch (UnsupportedEncodingException e) {
			throw new MemcachedException(
					"Unsupport encoding utf-8 when decodeKey", e);
		}
	}

	private String sanitizeKey(String key) throws MemcachedException {
		try {
			return sanitizeKeys ? URLEncoder.encode(key, "UTF-8") : key;
		} catch (UnsupportedEncodingException e) {
			throw new MemcachedException(
					"Unsupport encoding utf-8 when sanitizeKey", e);
		}
	}

	public Counter getCounter(String key, long initialValue) {
		return new Counter(this, key, initialValue);
	}

	public Counter getCounter(String key) {
		return new Counter(this, key, 0);
	}

	@SuppressWarnings("unchecked")
	public KeyIterator getKeyIterator(InetSocketAddress address)
			throws MemcachedException, TimeoutException, InterruptedException {
		if (address == null) {
			throw new IllegalArgumentException("null address");
		}
		Queue<Session> sessions = connector.getSessionByAddress(address);
		if (sessions == null || sessions.size() == 0) {
			throw new MemcachedException(
					"The special memcached server has not been connected,"
							+ address);
		}
		Session session = sessions.peek();
		CountDownLatch latch = new CountDownLatch(1);
		Command command = commandFactory.createStatsCommand(session
				.getRemoteSocketAddress(), latch, "items");
		session.write(command);
		if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Operation timeout");
		}
		Map<String, String> result = (Map<String, String>) command.getResult();
		LinkedList<Integer> itemNumberList = new LinkedList<Integer>();
		for (Map.Entry<String, String> entry : result.entrySet()) {
			final String key = entry.getKey();
			final String[] keys = key.split(":");
			if (keys.length == 3 && keys[2].equals("number")
					&& keys[0].equals("items")) {
				// has items,then add it to itemNumberList
				if (Integer.parseInt(entry.getValue()) > 0) {
					itemNumberList.add(Integer.parseInt(keys[1]));
				}
			}
		}
		return new KeyIteratorImpl(itemNumberList, this, address);
	}

}
