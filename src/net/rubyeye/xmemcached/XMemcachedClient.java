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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;

/**
 * 核心类，客户端应用类
 *
 * @author dennis(killme2008@gmail.com)
 *
 */
public class XMemcachedClient {

	/**
	 * 默认的读buffer线程数
	 */
	public static final int DEFAULT_READ_THREAD_COUNT = 0;
	/**
	 * 默认的连接超时,30秒
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 30000;
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
	public void setMergeFactor(int mergeFactor) {
		if (mergeFactor < 0)
			throw new IllegalArgumentException("mergeFactor<0");
		this.connector.setMergeFactor(mergeFactor);
	}

	/**
	 * 返回连接管理器
	 *
	 * @return
	 */
	public MemcachedConnector getConnector() {
		return connector;
	}

	/**
	 * 设置是否启用get优化，xmemcached会将连续的get操作尽可能合并成一个getMulti操作，默认开启
	 *
	 * @param optimiezeGet
	 */
	public void setOptimiezeGet(boolean optimiezeGet) {
		this.connector.setOptimiezeGet(optimiezeGet);
	}

	/**
	 * 是否启用缓存区合并优化，xmemcached会尽可能将连续的命令合并起来，以形成一个socket.getSendBufferSize()
	 * 大小的packet发出，默认开启
	 *
	 * @param optimizeMergeBuffer
	 */
	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		this.connector.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

	/**
	 * 是否已经关闭xmemcached客户端
	 *
	 * @return
	 */
	public boolean isShutdown() {
		return shutdown;
	}

	private final boolean sendCommand(Command cmd) throws InterruptedException,
			MemcachedException {
		if (this.shutdown) {
			throw new IllegalStateException("Xmemcached is stopped");
		}
		return connector.send(cmd);
	}

	private MemcachedConnector connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	private MemcachedHandler memcachedHandler;

	public XMemcachedClient(String server, int port) throws IOException {
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

	private void connect(InetSocketAddress inetSocketAddress) {
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

	private void startConnector() throws IOException {
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
	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		CommandFactory.setBufferAllocator(bufferAllocator);
	}

	public XMemcachedClient(Configuration configuration) throws IOException {
		this(new ArrayMemcachedSessionLocator(), new SimpleBufferAllocator(),
				configuration);

	}

	public XMemcachedClient(Configuration configuration,
			BufferAllocator allocator) throws IOException {
		this(new ArrayMemcachedSessionLocator(), allocator, configuration);

	}

	public XMemcachedClient(Configuration configuration,
			MemcachedSessionLocator locator) throws IOException {
		this(locator, new SimpleBufferAllocator(), configuration);

	}

	public static final Configuration getDefaultConfiguration() {
		Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(DEFAULT_TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(DEFAULT_SESSION_READ_BUFF_SIZE);
		configuration.setTcpNoDelay(DEFAULT_TCP_NO_DELAY);
		configuration.setReadThreadCount(DEFAULT_READ_THREAD_COUNT);
		return configuration;
	}

	public XMemcachedClient(InetSocketAddress inetSocketAddress)
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

	public XMemcachedClient(BufferAllocator allocator) throws IOException {
		this(new ArrayMemcachedSessionLocator(), allocator,
				getDefaultConfiguration());

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
	public <T> T get(final String key, long timeout, Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return (T) get0(key, timeout, ByteUtils.GET,
				Command.CommandType.GET_ONE, transcoder);
	}

	@SuppressWarnings("unchecked")
	public Object get(final String key, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return get(key, timeout, this.transcoder);
	}

	public <T> T get(final String key, Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		return get(key, DEFAULT_OP_TIMEOUT, transcoder);
	}

	public Object get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return get(key, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	private <T> Object get0(final String key, long timeout, byte[] cmdBytes,
			Command.CommandType cmdType, Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		final Command command = CommandFactory.createGetCommand(key, cmdBytes,
				cmdType);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, timeout);
		command.getIoBuffer().free(); // free buffer
		if (command.getException() != null) {
			throw command.getException();
		}
		CachedData data = (CachedData) command.getResult();
		if (data == null) {
			return null;
		}
		if (transcoder == null)
			transcoder = this.transcoder;
		if (cmdType == Command.CommandType.GETS_ONE) {
			return new GetsResponse<T>(data.getCas(), transcoder.decode(data));
		} else {
			return transcoder.decode(data);
		}
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
	public <T> GetsResponse<T> gets(final String key, long timeout,
			Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (GetsResponse<T>) get0(key, timeout, ByteUtils.GETS,
				Command.CommandType.GETS_ONE, transcoder);
	}

	public <T> GetsResponse<T> gets(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(key, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public <T> GetsResponse<T> gets(final String key, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return gets(key, timeout, this.transcoder);
	}

	@SuppressWarnings("unchecked")
	public <T> GetsResponse<T> gets(final String key, Transcoder transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
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
	public <T> Map<String, T> get(Collection<String> keyCollections,
			long timeout, Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return getMulti0(keyCollections, timeout, ByteUtils.GET,
				Command.CommandType.GET_MANY, transcoder);
	}

	public <T> Map<String, T> get(Collection<String> keyCollections,
			Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return getMulti0(keyCollections, lazy * DEFAULT_OP_TIMEOUT,
				ByteUtils.GET, Command.CommandType.GET_MANY, transcoder);
	}

	public <T> Map<String, T> get(Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return get(keyCollections, lazy * DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public <T> Map<String, T> get(Collection<String> keyCollections,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return (Map<String, T>) get(keyCollections, timeout,
				this.transcoder);
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
	public <T> Map<String, GetsResponse<T>> gets(
			Collection<String> keyCollections, long timeout,
			Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (Map<String, GetsResponse<T>>) getMulti0(keyCollections,
				timeout, ByteUtils.GETS, Command.CommandType.GETS_MANY,
				transcoder);
	}

	@SuppressWarnings("unchecked")
	public Map<String, GetsResponse> gets(Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return gets(keyCollections, 3 * DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public Map<String, GetsResponse> gets(Collection<String> keyCollections,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return gets(keyCollections, timeout, this.transcoder);
	}

	@SuppressWarnings("unchecked")
	public Map<String, GetsResponse> gets(Collection<String> keyCollections,
			Transcoder transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return gets(keyCollections, DEFAULT_OP_TIMEOUT, transcoder);
	}

	private <T> Map<String, T> getMulti0(Collection<String> keyCollections,
			long timeout, byte[] cmdBytes, Command.CommandType cmdType,
			Transcoder<T> transcoder) throws TimeoutException,
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
			if (getCmd.getException() != null) {
				throw getCmd.getException();
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
	private Collection<List<String>> catalogKeys(
			Collection<String> keyCollections) {
		Map<Session, List<String>> catalogMap = new HashMap<Session, List<String>>();

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

	private <T> Command sendGetMultiCommand(List<String> keys,
			CountDownLatch latch, Map<String, T> result, byte[] cmdBytes,
			Command.CommandType cmdType, Transcoder<T> transcoder)
			throws InterruptedException, TimeoutException, MemcachedException {
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
	public <T> boolean set(final String key, final int exp, T value,
			Transcoder<T> transcoder, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", timeout, -1, transcoder);
	}

	public boolean set(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return set(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public boolean set(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return set(key, exp, value, this.transcoder, timeout);
	}

	public <T> boolean set(final String key, final int exp, T value,
			Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
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
	public <T> boolean add(final String key, final int exp, T value,
			Transcoder<T> transcoder, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"add", timeout, -1, transcoder);
	}

	public boolean add(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return add(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public boolean add(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return add(key, exp, value, this.transcoder, timeout);
	}


	public <T> boolean add(final String key, final int exp, T value,
			Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
		return add(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT);
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return replace(key, exp, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public boolean replace(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return replace(key, exp, value, this.transcoder, timeout);
	}

	public <T> boolean replace(final String key, final int exp, T value,
			Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException {
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
	public <T> boolean replace(final String key, final int exp, T value,
			Transcoder<T> transcoder, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
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
	public boolean append(final String key, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return append(key, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public boolean append(final String key, Object value, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
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
	public boolean prepend(final String key, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		return prepend(key, value, DEFAULT_OP_TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public boolean prepend(final String key, Object value, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, 0, value, Command.CommandType.PREPEND,
				"prepend", timeout, -1, this.transcoder);
	}

	public boolean cas(final String key, final int exp, Object value, long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		return cas(key, exp, value, DEFAULT_OP_TIMEOUT, cas);
	}

	@SuppressWarnings("unchecked")
	public boolean cas(final String key, final int exp, Object value,
			long timeout, long cas) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, value, this.transcoder, timeout, cas);
	}

	public <T> boolean cas(final String key, final int exp, T value,
			Transcoder<T> transcoder, long cas) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, value, transcoder, DEFAULT_OP_TIMEOUT, cas);
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
	public <T> boolean cas(final String key, final int exp, T value,
			Transcoder<T> transcoder, long timeout, long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.CAS,
				"cas", timeout, cas, transcoder);
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
	public <T> boolean cas(final String key, final int exp,
			CASOperation<T> operation, Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		if (operation == null) {
			throw new IllegalArgumentException("CASOperation could not be null");
		}
		if (operation.getMaxTries() < 0) {
			throw new IllegalArgumentException(
					"max tries must be greater than 0");
		}
		int tryCount = 0;
		GetsResponse<T> result = gets(key);
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
			result = gets(key);
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
	public <T> boolean cas(final String key, final int exp,
			CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException {
		return cas(key, exp, operation, this.transcoder);
	}

	public boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		final Command command = CommandFactory.createDeleteCommand(key, time);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT);
		command.getIoBuffer().free();
		if (command.getException() != null) {
			throw command.getException();
		}
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Boolean) command.getResult();
	}

	public final String version() throws TimeoutException,
			InterruptedException, MemcachedException {
		final Command command = CommandFactory.createVersionCommand();
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT);
		command.getIoBuffer().free(); // free buffer
		if (command.getException() != null) {
			throw command.getException();
		}
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (String) command.getResult();
	}

	public int incr(final String key, final int num) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendIncrOrDecrCommand(key, num, Command.CommandType.INCR, "incr");
	}

	public int decr(final String key, final int num) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendIncrOrDecrCommand(key, num, Command.CommandType.DECR, "decr");
	}

	public void shutdown() throws IOException {
		if (this.shutdown) {
			return;
		}
		this.shutdown = true;
		this.connector.stop();
	}

	private int sendIncrOrDecrCommand(final String key, final int num,
			Command.CommandType cmdType, final String cmd)
			throws InterruptedException, TimeoutException, MemcachedException {
		final Command command = CommandFactory.createIncrDecrCommand(key, num,
				cmdType, cmd);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT);
		command.getIoBuffer().free();
		if (command.getException() != null) {
			throw command.getException();
		}
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

	public boolean delete(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return delete(key, 0);
	}

	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder() {
		return transcoder;
	}

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
		this.memcachedHandler.setTranscoder(transcoder);
	}

	public MemcachedHandler getMemcachedHandler() {
		return memcachedHandler;
	}

	@SuppressWarnings("unchecked")
	private <T> boolean sendStoreCommand(final String key, final int exp,
			final T value, Command.CommandType cmdType, final String cmd,
			long timeout, long cas, Transcoder<T> transcoder)
			throws InterruptedException, TimeoutException, MemcachedException {
		if (value == null) {
			throw new IllegalArgumentException("value could not be null");
		}
		if (exp < 0) {
			throw new IllegalArgumentException(
					"Expire time must be greater than 0");
		}
		if (transcoder == null)
			transcoder = this.transcoder;
		final Command command = CommandFactory.createStoreCommand(key, exp,
				value, cmdType, cmd, cas, transcoder);

		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, timeout);
		command.getIoBuffer().free();
		if (command.getException() != null) {
			throw command.getException();
		}
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Boolean) command.getResult();
	}

	private void latchWait(Command cmd, long timeout)
			throws InterruptedException, TimeoutException {
		if (!cmd.getLatch().await(timeout, TimeUnit.MILLISECONDS)) {
			cmd.cancel();
			throw new TimeoutException("Timed out waiting for operation");
		}
	}
}
