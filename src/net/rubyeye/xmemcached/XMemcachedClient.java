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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.IOException;

import net.rubyeye.memcached.transcoders.CachedData;
import net.rubyeye.memcached.transcoders.SerializingTranscoder;
import net.rubyeye.memcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.command.Command.CommandType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
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
public final class XMemcachedClient {

	/**
	 * 默认的读buffer线程数
	 */
	public static final int DEFAULT_READ_THREAD_COUNT = 0;
	/**
	 * 默认的连接超时,1分钟
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60000;
	/**
	 * 默认的socket发送缓冲区大小，16K
	 */
	public static final int DEFAULT_TCP_SEND_BUFF_SIZE = 16 * 1024;
	/**
	 * 默认启用Nagle算法
	 */
	public static final boolean DEFAULT_TCP_NO_DELAY = false;
	/**
	 * 默认的session内的缓冲区大小，32K
	 */
	public static final int DEFAULT_SESSION_READ_BUFF_SIZE = 32 * 1024;
	/**
	 * 默认的Socket接收缓冲区大小，16K
	 */
	public static final int DEFAULT_TCP_RECV_BUFF_SIZE = 16 * 1024;
	/**
	 * 默认的操作超时时间，1秒
	 */
	public static final long DEFAULT_OP_TIMEOUT = 1000;
	private static final Log log = LogFactory.getLog(XMemcachedClient.class);
	private MemcachedSessionLocator sessionLocator;
	private volatile boolean shutdown;

	/**
	 * 设置合并系数，这一参数影响get优化和合并缓存区优化，这个系数决定最多的合并command数，默认是150
	 * 
	 * @param mergeFactor
	 */
	public final void setMergeFactor(final int mergeFactor) {
		if (mergeFactor < 0) {
			throw new IllegalArgumentException("mergeFactor<0");
		}
		this.connector.setMergeFactor(mergeFactor);
	}

	/**
	 * 返回连接管理器
	 * 
	 * @return
	 */
	public final MemcachedConnector getConnector() {
		return connector;
	}

	/**
	 * 设置是否启用get优化，xmemcached会将连续的get操作尽可能合并成一个getMulti操作，默认开启
	 * 
	 * @param optimiezeGet
	 */
	public final void setOptimiezeGet(final boolean optimiezeGet) {
		this.connector.setOptimiezeGet(optimiezeGet);
	}

	/**
	 * 是否启用缓存区合并优化，xmemcached会尽可能将连续的命令合并起来，以形成一个socket.getSendBufferSize()
	 * 大小的packet发出，默认开启
	 * 
	 * @param optimizeMergeBuffer
	 */
	public final void setOptimizeMergeBuffer(final boolean optimizeMergeBuffer) {
		this.connector.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	/**
	 * 是否已经关闭xmemcached客户端
	 * 
	 * @return
	 */
	public final boolean isShutdown() {
		return shutdown;
	}

	@SuppressWarnings("unchecked")
	private final <T> GetsResponse<T> gets0(final String key,
			final byte[] keyBytes, final Transcoder<T> transcoder)
			throws MemcachedException, TimeoutException, InterruptedException {
		GetsResponse<T> result = (GetsResponse<T>) fetch0(key, keyBytes,
				ByteUtils.GETS, Command.CommandType.GETS_ONE,
				DEFAULT_OP_TIMEOUT, transcoder);
		return result;
	}

	private final boolean sendCommand(final Command cmd)
			throws MemcachedException {
		if (this.shutdown) {
			throw new IllegalStateException("Xmemcached is stopped");
		}
		return connector.send(cmd);
	}

	private MemcachedConnector connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	private MemcachedHandler memcachedHandler;

	public XMemcachedClient(final String server, final int port)
			throws IOException {
		super();
		checkServerPort(server, port);
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration());
		startConnector();
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

	/**
	 * 添加memcached节点
	 * 
	 * @param server
	 * @param port
	 * @throws IOException
	 */
	public final void addServer(final String server, final int port)
			throws IOException {
		checkServerPort(server, port);
		connect(new InetSocketAddress(server, port));
	}

	/**
	 * 添加memcached节点
	 * 
	 * @param inetSocketAddress
	 */
	public final void addServer(final InetSocketAddress inetSocketAddress) {
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		connect(inetSocketAddress);
	}

	private void connect(final InetSocketAddress inetSocketAddress) {
		Future<Boolean> future = null;
		boolean connected = false;
		try {
			future = this.connector.connect(inetSocketAddress);

			if (!future.isDone()
					&& !future.get(DEFAULT_CONNECT_TIMEOUT,
							TimeUnit.MILLISECONDS)) {
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
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " error", e);
		} catch (TimeoutException e) {
			if (future != null) {
				future.cancel(true);
			}
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " timeout", e);
		} catch (Exception e) {
			if (future != null) {
				future.cancel(true);
			}
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " error", e);
		}
		if (!connected) {
			this.connector
					.addToWatingQueue(new MemcachedConnector.ReconnectRequest(
							inetSocketAddress, 0));

		}
	}

	@SuppressWarnings("unchecked")
	private final <T> Object fetch0(final String key, final byte[] keyBytes,
			final byte[] cmdBytes, final CommandType cmdType,
			final long timeout, Transcoder<T> transcoder)
			throws InterruptedException, TimeoutException, MemcachedException,
			MemcachedException {
		final Command command = CommandFactory.createGetCommand(key, keyBytes,
				cmdBytes, cmdType);
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
		if (cmdType == Command.CommandType.GETS_ONE) {
			return new GetsResponse<T>(data.getCas(), transcoder.decode(data));
		} else {
			return transcoder.decode(data);
		}
	}

	private final void startConnector() throws IOException {
		if (this.shutdown) {
			this.connector.start();
			this.shutdown = false;
		}
	}

	private void buildConnector(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration configuration) {
		if (locator == null) {
			locator = new ArrayMemcachedSessionLocator();

		}
		if (allocator == null) {
			allocator = new SimpleBufferAllocator();
		}
		if (configuration == null) {
			configuration = getDefaultConfiguration();
		}

		CommandFactory.setBufferAllocator(allocator);
		this.shutdown = true;
		this.transcoder = new SerializingTranscoder();
		this.sessionLocator = locator;
		this.connector = new MemcachedConnector(configuration, sessionLocator,
				allocator);
		this.connector.setSendBufferSize(DEFAULT_TCP_SEND_BUFF_SIZE);
		this.memcachedHandler = new MemcachedHandler(this.transcoder, this);
		this.connector.setHandler(memcachedHandler);
		this.connector.setMemcachedProtocolHandler(memcachedHandler);
	}

	/**
	 * 设置IoBuffer分配器，默认采用SimpleBufferAllocator
	 * 
	 * @param bufferAllocator
	 * @return
	 */
	public final void setBufferAllocator(final BufferAllocator bufferAllocator) {
		CommandFactory.setBufferAllocator(bufferAllocator);
	}

	public XMemcachedClient(final Configuration configuration)
			throws IOException {
		this(new ArrayMemcachedSessionLocator(), new SimpleBufferAllocator(),
				configuration);

	}

	public XMemcachedClient(final Configuration configuration,
			final BufferAllocator allocator) throws IOException {
		this(new ArrayMemcachedSessionLocator(), allocator, configuration);

	}

	public XMemcachedClient(final Configuration configuration,
			final MemcachedSessionLocator locator) throws IOException {
		this(locator, new SimpleBufferAllocator(), configuration);

	}

	public static final Configuration getDefaultConfiguration() {
		final Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(DEFAULT_TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(DEFAULT_SESSION_READ_BUFF_SIZE);
		configuration.setTcpNoDelay(DEFAULT_TCP_NO_DELAY);
		configuration.setReadThreadCount(DEFAULT_READ_THREAD_COUNT);
		return configuration;
	}

	public XMemcachedClient(final InetSocketAddress inetSocketAddress)
			throws IOException {
		super();
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration());
		startConnector();
		connect(inetSocketAddress);
	}

	public XMemcachedClient() throws IOException {
		super();
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration());
		startConnector();
	}

	public XMemcachedClient(MemcachedSessionLocator locator) throws IOException {
		this(locator, new SimpleBufferAllocator(), getDefaultConfiguration());
	}

	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf) throws IOException {
		super();
		buildConnector(locator, allocator, conf);
		startConnector();
	}

	public XMemcachedClient(MemcachedSessionLocator locator,
			BufferAllocator allocator, Configuration conf,
			List<InetSocketAddress> addressList) throws IOException {
		super();
		buildConnector(locator, allocator, conf);
		startConnector();
		if (addressList != null) {
			for (InetSocketAddress inetSocketAddress : addressList) {
				connect(inetSocketAddress);
			}
		}
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
		buildConnector(new ArrayMemcachedSessionLocator(),
				new SimpleBufferAllocator(), getDefaultConfiguration());
		startConnector();
		for (InetSocketAddress inetSocketAddress : addressList) {
			connect(inetSocketAddress);
		}
	}

	/**
	 * 获取key对应的缓存项
	 * 
	 * @param <T>
	 * @param key
	 *            关键字key
	 * @param timeout
	 *            操作的超时时间，单位毫秒
	 * @param transcoder
	 *            缓存项的转换器，如果为null就默认使用内部的转换器负责判断类型并反序列化
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (T) get0(key, timeout, ByteUtils.GET,
				Command.CommandType.GET_ONE, transcoder);
	}

	@SuppressWarnings("unchecked")
	public final Object get(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(key, timeout, this.transcoder);
	}

	public final <T> T get(final String key, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(key, DEFAULT_OP_TIMEOUT, transcoder);
	}

	public final Object get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return get(key, DEFAULT_OP_TIMEOUT);
	}

	private <T> Object get0(final String key, final long timeout,
			final byte[] cmdBytes, final Command.CommandType cmdType,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		return fetch0(key, keyBytes, cmdBytes, cmdType, timeout, transcoder);
	}

	/**
	 * 类似get,但是gets将返回缓存项的cas值，可用于cas操作，参见cas方法
	 * 
	 * @param <T>
	 * @param key
	 * @param timeout
	 * @param transcoder
	 * @return GetsResponse
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (GetsResponse<T>) get0(key, timeout, ByteUtils.GETS,
				Command.CommandType.GETS_ONE, transcoder);
	}

	public final <T> GetsResponse<T> gets(final String key)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(key, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(key, timeout, this.transcoder);
	}

	@SuppressWarnings("unchecked")
	public final <T> GetsResponse<T> gets(final String key,
			final Transcoder transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(key, DEFAULT_OP_TIMEOUT, transcoder);
	}

	/**
	 * memcached的getMulti，批量获取一批key对应的缓存项
	 * 
	 * @param <T>
	 * @param keyCollections
	 *            关键字集合
	 * @param timeout
	 *            操作超时
	 * @param transcoder
	 *            转换器
	 * @return map对象，存储存在的缓存项
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return getMulti0(keyCollections, timeout, ByteUtils.GET,
				Command.CommandType.GET_MANY, transcoder);
	}

	public final <T> Map<String, T> get(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return getMulti0(keyCollections, lazy * DEFAULT_OP_TIMEOUT,
				ByteUtils.GET, Command.CommandType.GET_MANY, transcoder);
	}

	public final <T> Map<String, T> get(final Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return get(keyCollections, lazy * DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return (Map<String, T>) get(keyCollections, timeout, this.transcoder);
	}

	/**
	 * 类似getMulti，但是返回缓存项的cas值，返回的map中value存储的是GetsResponse对象
	 * 
	 * @param <T>
	 * @param keyCollections
	 * @param timeout
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@SuppressWarnings("unchecked")
	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (Map<String, GetsResponse<T>>) getMulti0(keyCollections,
				timeout, ByteUtils.GETS, Command.CommandType.GETS_MANY,
				transcoder);
	}

	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return gets(keyCollections, lazy * DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(keyCollections, timeout, this.transcoder);
	}

	public final <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(keyCollections, DEFAULT_OP_TIMEOUT, transcoder);
	}

	private <T> Map<String, T> getMulti0(
			final Collection<String> keyCollections, final long timeout,
			final byte[] cmdBytes, final Command.CommandType cmdType,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		if (keyCollections == null || keyCollections.size() == 0) {
			return null;
		}
		Collection<List<String>> catalogKeys = catalogKeys(keyCollections);
		Map<String, T> result = new HashMap<String, T>();
		List<Command> commands = new LinkedList<Command>();
		final CountDownLatch latch = new CountDownLatch(catalogKeys.size());
		for (List<String> keys : catalogKeys) {
			commands.add(sendGetMultiCommand(keys, latch, result, cmdBytes,
					cmdType, transcoder));
		}
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy;
		if (!latch.await(timeout * lazy, TimeUnit.MILLISECONDS)) {
			for (Command getCmd : commands) {
				getCmd.cancel();
			}
			throw new TimeoutException("Timed out waiting for operation");
		}
		for (Command getCmd : commands) {
			getCmd.getIoBuffer().free();
			checkException(getCmd);
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
				List<String> tmpKeys = new LinkedList<String>();
				tmpKeys.add(key);
				catalogMap.put(index, tmpKeys);
			} else {
				catalogMap.get(index).add(key);
			}
		}

		Collection<List<String>> catalogKeys = catalogMap.values();
		return catalogKeys;
	}

	private <T> Command sendGetMultiCommand(final List<String> keys,
			final CountDownLatch latch, final Map<String, T> result,
			final byte[] cmdBytes, final Command.CommandType cmdType,
			final Transcoder<T> transcoder) throws InterruptedException,
			TimeoutException, MemcachedException {
		final Command command = CommandFactory.createGetMultiCommand(keys,
				latch, result, cmdBytes, cmdType, transcoder);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		return command;
	}

	/**
	 * 设置key对应的项为value，无论key是否已经存在，成功返回true，否则返回false
	 * 
	 * @param <T>
	 * @param key
	 *            缓存关键字
	 * @param exp
	 *            缓存的超时时间
	 * @param value
	 *            缓存的值对象
	 * @param transcoder
	 *            对象转换器
	 * @param timeout
	 *            操作的超时时间，单位是毫秒
	 * @return
	 * @throws TimeoutException
	 *             操作超时抛出此异常
	 * @throws InterruptedException
	 *             操作被中断
	 * @throws MemcachedException
	 *             memcached异常，可能是客户端或者memcached server返回的错误
	 */
	public final <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", timeout, -1, transcoder);
	}

	public final boolean set(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return set(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final boolean set(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return set(key, exp, value, this.transcoder, timeout);
	}

	public final <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return set(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	/**
	 *添加key-value缓存项，仅在key不存在的情况下才能添加成功，成功返回true，否则返回false
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            缓存的超时时间，0为永不过期（memcached默认为一个月）
	 * @param value
	 *            缓存的值对象
	 * @param transcoder
	 *            值对象的转换器
	 * @param timeout
	 *            操作的超时时间，单位毫秒
	 * @return
	 * @throws TimeoutException
	 *             操作超时抛出此异常
	 * @throws InterruptedException
	 *             操作被中断
	 * @throws MemcachedException
	 *             memcached异常，可能是客户端或者memcached server返回的错误
	 */
	public final <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"add", timeout, -1, transcoder);
	}

	public final boolean add(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final boolean add(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return add(key, exp, value, this.transcoder, timeout);
	}

	public final <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	public final boolean replace(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException {
		return replace(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final boolean replace(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return replace(key, exp, value, this.transcoder, timeout);
	}

	public final <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return replace(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	/**
	 * 替代key对应的值，当且仅当key对应的缓存项存在的时候可以替换，如果key不存在返回false，如果替代成功返回true
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            缓存的超时时间
	 * @param value
	 *            值对象
	 * @param transcoder
	 *            值对象的转换器
	 * @param timeout
	 *            操作的超时时间,单位毫秒
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"replace", timeout, -1, transcoder);
	}

	/**
	 * 将value添加到key对应的缓存项后面连接起来，这一操作仅对String有意义。
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return append(key, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final boolean append(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {

		return sendStoreCommand(key, 0, value, Command.CommandType.APPEND,
				"append", timeout, -1, this.transcoder);
	}

	/**
	 * 类似append，是将value附加到key对应的缓存项前面，这一操作仅对String有实际意义
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final boolean prepend(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return prepend(key, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public final boolean prepend(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return sendStoreCommand(key, 0, value, Command.CommandType.PREPEND,
				"prepend", timeout, -1, this.transcoder);
	}

	public final boolean cas(final String key, final int exp,
			final Object value, final long cas) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, value, DEFAULT_OP_TIMEOUT, cas);
	}

	/**
	 * cas原子替换key对应的value，当且仅当cas值相等的时候替换成功
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout,
			final long cas) throws TimeoutException, InterruptedException,
			MemcachedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.CAS,
				"cas", timeout, cas, transcoder);
	}

	@SuppressWarnings("unchecked")
	public final boolean cas(final String key, final int exp,
			final Object value, final long timeout, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		return cas(key, exp, value, this.transcoder, timeout, cas);
	}

	public final <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		return cas(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT, cas);
	}

	/**
	 * 原子替换key对应的value值，当且仅当cas值相等时替换成功，具体使用参见wiki
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            操作的超时时间
	 * @param operation
	 *            CASOperation对象，包装cas操作
	 * @param transcoder
	 *            对象转换器
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		if (operation == null) {
			throw new IllegalArgumentException("CASOperation could not be null");
		}
		if (operation.getMaxTries() < 0) {
			throw new IllegalArgumentException(
					"max tries must be greater than 0");
		}
		int tryCount = 0;
		GetsResponse<T> result = gets0(key, keyBytes, transcoder);
		if (result == null) {
			throw new MemcachedException("could not found the value for Key="
					+ key);
		}
		while (tryCount < operation.getMaxTries()
				&& result != null
				&& !sendStoreCommand(key, exp, operation.getNewValue(result
						.getCas(), result.getValue()), Command.CommandType.CAS,
						"cas", DEFAULT_OP_TIMEOUT, result.getCas(), transcoder)) {
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

	@SuppressWarnings("unchecked")
	public final <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, operation, this.transcoder);
	}

	public final boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = CommandFactory.createDeleteCommand(key,
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

	public final String version() throws TimeoutException,
			InterruptedException, MemcachedException {
		final Command command = CommandFactory.createVersionCommand();
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

	public final int incr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendIncrOrDecrCommand(key, num, Command.CommandType.INCR, "incr");
	}

	public final int decr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException {
		return sendIncrOrDecrCommand(key, num, Command.CommandType.DECR, "decr");
	}

	/**
	 * 使cache中所有的数据项失效，如果是连接多个节点的memcached，那么所有的memcached中的数据项都将失效
	 * 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final void flushAll() throws TimeoutException, InterruptedException,
			MemcachedException {
		flushAll(DEFAULT_OP_TIMEOUT);
	}

	/**
	 * 使cache中所有的数据项失效,如果是连接多个节点的memcached，那么所有的memcached中的数据项都将失效
	 * 
	 * @param timeout
	 *            操作超时时间
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		final Collection<Session> sessions = this.connector.getSessionSet();
		CountDownLatch latch = new CountDownLatch(sessions.size());
		for (Session session : sessions) {
			if (session != null && !session.isClosed()) {
				Command command = CommandFactory.createFlushAllCommand();
				command.setLatch(latch);
				session.send(command);
			} else
				latch.countDown();
		}
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
	}

	/**
	 * 使指定memcached节点的数据项失效
	 * 
	 * @param host
	 *            memcached节点host ip:port的形式
	 * @param timeout
	 *            操作超时时间
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final void flushAll(String host, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		InetSocketAddress address = AddrUtil.getAddress(host);
		flushAll(address, timeout);
	}

	public final void flushAll(InetSocketAddress address) throws MemcachedException,
			InterruptedException, TimeoutException {
		flushAll(address, DEFAULT_OP_TIMEOUT);
	}

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
		Command command = CommandFactory.createFlushAllCommand();
		command.setLatch(latch);
		if (!session.send(command))
			throw new MemcachedException("send command fail");
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
	}

	/**
	 * 使指定memcached节点的数据项失效
	 * 
	 * @param host
	 *            memcached节点host ip:port的形式
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final void flushAll(String host) throws TimeoutException,
			InterruptedException, MemcachedException {
		flushAll(host, DEFAULT_OP_TIMEOUT);
	}

	/**
	 * 查看特定节点的memcached server统计信息
	 * 
	 * @param host
	 *            memcached节点host ip:port的形式
	 * @param timeout
	 *            操作超时
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public final Map<String, String> stats(String host, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		InetSocketAddress address = AddrUtil.getAddress(host);
		return stats(address, timeout);
	}

	public final Map<String, String> stats(String host) throws TimeoutException,
			InterruptedException, MemcachedException {
		return stats(host, DEFAULT_OP_TIMEOUT);
	}

	public final Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException {
		return stats(address, DEFAULT_OP_TIMEOUT);
	}

	/**
	 * 查看特定节点的memcached server统计信息
	 * 
	 * @param address
	 *            节点地址
	 * @param timeout
	 *            操作超时
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public final Map<String, String> stats(InetSocketAddress address, long timeout)
			throws MemcachedException, InterruptedException, TimeoutException {
		if (address == null)
			throw new IllegalArgumentException("Null inetSocketAddress");
		CountDownLatch latch = new CountDownLatch(1);

		Session session = this.connector.getSessionByAddress(address);
		if (session == null)
			throw new MemcachedException("could not find session for "
					+ address.getHostName() + ":" + address.getPort()
					+ ",maybe it have not been connected");
		Map<String, String> result = new HashMap<String, String>();
		Command command = CommandFactory.createStatsCommand();
		command.setResult(result);
		command.setLatch(latch);
		if (!session.send(command))
			throw new MemcachedException("send command fail");
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		return result;
	}

	public final void shutdown() throws IOException {
		if (this.shutdown) {
			return;
		}
		this.shutdown = true;
		this.connector.stop();
		XMemcachedMbeanServer.getInstance().shutdown();
	}

	private final int sendIncrOrDecrCommand(final String key, final int num,
			final Command.CommandType cmdType, final String cmd)
			throws InterruptedException, TimeoutException, MemcachedException {
		final byte[] keyBytes = ByteUtils.getBytes(key);
		ByteUtils.checkKey(keyBytes);
		final Command command = CommandFactory.createIncrDecrCommand(key,
				keyBytes, num, cmdType, cmd);
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
		if (command.getResult() instanceof Boolean
				&& !((Boolean) command.getResult())) {
			return -1;
		} else {
			return (Integer) command.getResult();
		}
	}

	public final boolean delete(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return delete(key, 0);
	}

	@SuppressWarnings("unchecked")
	public final Transcoder getTranscoder() {
		return transcoder;
	}

	@SuppressWarnings("unchecked")
	public final void setTranscoder(final Transcoder transcoder) {
		this.transcoder = transcoder;
		this.memcachedHandler.setTranscoder(transcoder);
	}

	public final MemcachedHandler getMemcachedHandler() {
		return memcachedHandler;
	}

	@SuppressWarnings("unchecked")
	private <T> boolean sendStoreCommand(final String key, final int exp,
			final T value, final Command.CommandType cmdType, final String cmd,
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
		final Command command = CommandFactory.createStoreCommand(key,
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
