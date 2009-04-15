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
import java.nio.ByteBuffer;
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
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleIoBuffer;
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

	public static final int DEFAULT_READ_THREAD_COUNT = 0;
	public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
	public static final int DEFAULT_TCP_SEND_BUFF_SIZE = 16 * 1024;
	public static final boolean DEFAULT_TCP_NO_DELAY = false;
	public static final int DEFAULT_SESSION_READ_BUFF_SIZE = 32 * 1024;
	public static final int DEFAULT_TCP_RECV_BUFF_SIZE = 16 * 1024;
	public static final long DEFAULT_OP_TIMEOUT = 1000;
	private static final Log log = LogFactory.getLog(XMemcachedClient.class);
	private MemcachedSessionLocator sessionLocator;
	private volatile boolean shutdown;

	public void setMergeFactor(int mergeFactor) {
		this.connector.setMergeFactor(mergeFactor);
	}

	public MemcachedConnector getConnector() {
		return connector;
	}

	public void setOptimiezeGet(boolean optimiezeGet) {
		this.connector.setOptimiezeGet(optimiezeGet);
	}

	public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
		this.connector.setOptimizeMergeBuffer(optimizeMergeBuffer);
	}

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
	private BufferAllocator byteBufferAllocator;

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

	public final void addServer(final String server, final int port)
			throws IOException {
		checkServerPort(server, port);
		connect(new InetSocketAddress(server, port));
	}

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

		this.byteBufferAllocator = allocator;
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

	public Object get(final String key, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException {
		return get(key, timeout, ByteUtils.GET, Command.CommandType.GET_ONE);
	}

	public GetsResponse gets(final String key, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		return (GetsResponse) get(key, timeout, ByteUtils.GETS,
				Command.CommandType.GETS_ONE);
	}

	private Object get(final String key, long timeout, byte[] cmdBytes,
			Command.CommandType cmdType) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] keyBytes = ByteUtils.getBytes(key);
		final IoBuffer buffer = this.byteBufferAllocator
				.allocate(cmdBytes.length + ByteUtils.CRLF.length + 1
						+ keyBytes.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
		buffer.flip();
		Command getCmd = new Command(key, cmdType, latch);
		getCmd.setIoBuffer(buffer);
		if (!sendCommand(getCmd)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(getCmd, timeout, latch);
		buffer.free(); // free buffer
		if (getCmd.getException() != null) {
			throw getCmd.getException();
		}
		CachedData data = (CachedData) getCmd.getResult();
		if (data == null) {
			return null;
		}
		if (cmdType == Command.CommandType.GETS_ONE) {
			return new GetsResponse(data.getCas(), this.transcoder.decode(data));
		} else {
			return this.transcoder.decode(data);
		}
	}

	public Object get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return get(key, DEFAULT_OP_TIMEOUT, ByteUtils.GET,
				Command.CommandType.GET_ONE);
	}

	public GetsResponse gets(final String key) throws TimeoutException,
			InterruptedException, MemcachedException {
		return (GetsResponse) get(key, DEFAULT_OP_TIMEOUT, ByteUtils.GETS,
				Command.CommandType.GETS_ONE);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> get(Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return (Map<String, Object>) get(keyCollections, lazy
				* DEFAULT_OP_TIMEOUT, ByteUtils.GET,
				Command.CommandType.GET_MANY);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> get(Collection<String> keyCollections,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return (Map<String, Object>) get(keyCollections, timeout,
				ByteUtils.GET, Command.CommandType.GET_MANY);
	}

	@SuppressWarnings("unchecked")
	public Map<String, GetsResponse> gets(Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 3 ? 3 : lazy; // 最高3秒
		return (Map<String, GetsResponse>) get(keyCollections, lazy
				* DEFAULT_OP_TIMEOUT, ByteUtils.GETS,
				Command.CommandType.GETS_MANY);
	}

	@SuppressWarnings("unchecked")
	public Map<String, GetsResponse> gets(Collection<String> keyCollections,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		return (Map<String, GetsResponse>) get(keyCollections, timeout,
				ByteUtils.GETS, Command.CommandType.GETS_MANY);
	}

	@SuppressWarnings("unchecked")
	private Map get(Collection<String> keyCollections, long timeout,
			byte[] cmdBytes, Command.CommandType cmdType)
			throws TimeoutException, InterruptedException, MemcachedException {
		if (keyCollections == null || keyCollections.size() == 0) {
			return null;
		}
		Collection<List<String>> catalogKeys = catalogKeys(keyCollections);
		Map result = new HashMap();
		List<Command> commands = new LinkedList<Command>();
		final CountDownLatch latch = new CountDownLatch(catalogKeys.size());
		for (List<String> keys : catalogKeys) {
			commands.add(sendGetManyCommand(keys, latch, result, cmdBytes,
					cmdType));
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

	@SuppressWarnings("unchecked")
	private Command sendGetManyCommand(List<String> keys, CountDownLatch latch,
			Map result, byte[] cmdBytes, Command.CommandType cmdType)
			throws InterruptedException, TimeoutException, MemcachedException {
		Command command = new Command(keys.get(0), cmdType, latch);
		command.setResult(result); // 共用一个result map
		StringBuilder sb = new StringBuilder(keys.size() * 5);
		for (String tmpKey : keys) {
			ByteUtils.checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		byte[] keyBytes = ByteUtils.getBytes(sb.toString());
		final IoBuffer buffer = this.byteBufferAllocator
				.allocate(cmdBytes.length + ByteUtils.CRLF.length + 1
						+ keyBytes.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
		buffer.flip();
		command.setIoBuffer(buffer);
		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		return command;
	}

	public boolean set(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", DEFAULT_OP_TIMEOUT, -1);
	}

	public boolean set(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", timeout, -1);
	}

	public boolean add(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.ADD,
				"add", DEFAULT_OP_TIMEOUT, -1);
	}

	public boolean add(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"add", timeout, -1);
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"replace", DEFAULT_OP_TIMEOUT, -1);
	}

	public boolean replace(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"replace", timeout, -1);
	}

	public boolean append(final String key, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, 0, value, Command.CommandType.APPEND,
				"append", DEFAULT_OP_TIMEOUT, -1);
	}

	public boolean append(final String key, Object value, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, 0, value, Command.CommandType.APPEND,
				"append", timeout, -1);
	}

	public boolean prepend(final String key, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, 0, value, Command.CommandType.PREPEND,
				"prepend", DEFAULT_OP_TIMEOUT, -1);
	}

	public boolean prepend(final String key, Object value, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, 0, value, Command.CommandType.PREPEND,
				"prepend", timeout, -1);
	}

	public boolean cas(final String key, final int exp, Object value,
			long timeout, long cas) throws TimeoutException,
			InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.CAS,
				"cas", timeout, cas);
	}

	public boolean cas(final String key, final int exp, Object value, long cas)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.CAS,
				"cas", DEFAULT_OP_TIMEOUT, cas);
	}

	public boolean cas(final String key, final int exp, CASOperation operation)
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
		GetsResponse result = gets(key);
		if (result == null) {
			throw new MemcachedException("could not found the value for Key="
					+ key);
		}
		while (tryCount < operation.getMaxTries()
				&& result != null
				&& !sendStoreCommand(key, exp, operation.getNewValue(result
						.getCas(), result.getValue()), Command.CommandType.CAS,
						"cas", DEFAULT_OP_TIMEOUT, result.getCas())) {
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

	public boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] keyBytes = ByteUtils.getBytes(key);
		byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
		final IoBuffer buffer = this.byteBufferAllocator
				.allocate(ByteUtils.DELETE.length + 2 + keyBytes.length
						+ timeBytes.length + ByteUtils.CRLF.length);
		ByteUtils.setArguments(buffer, ByteUtils.DELETE, keyBytes, timeBytes);
		buffer.flip();
		Command command = new Command(key, Command.CommandType.DELETE, latch);
		command.setIoBuffer(buffer);

		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT, latch);
		buffer.free();
		if (command.getException() != null) {
			throw command.getException();
		}
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Boolean) command.getResult();
	}

	static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n".getBytes());

	public String version() throws TimeoutException, InterruptedException,
			MemcachedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final IoBuffer buffer = new SimpleIoBuffer(VERSION.slice());
		Command command = new Command("version", Command.CommandType.VERSION,
				latch);
		command.setIoBuffer(buffer);

		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT, latch);
		buffer.free(); // free buffer
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
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] numBytes = ByteUtils.getBytes(String.valueOf(num));
		byte[] cmdBytes = ByteUtils.getBytes(cmd);
		byte[] keyBytes = ByteUtils.getBytes(key);
		final IoBuffer buffer = this.byteBufferAllocator.allocate(cmd.length()
				+ 2 + key.length() + numBytes.length + ByteUtils.CRLF.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes, numBytes);
		buffer.flip();
		Command command = new Command(key, cmdType, latch);
		command.setIoBuffer(buffer);

		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, DEFAULT_OP_TIMEOUT, latch);
		buffer.free();
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
	private boolean sendStoreCommand(final String key, final int exp,
			final Object value, Command.CommandType cmdType, final String cmd,
			long timeout, long cas) throws InterruptedException,
			TimeoutException, MemcachedException {
		if (value == null) {
			throw new IllegalArgumentException("value could not be null");
		}
		if (exp < 0) {
			throw new IllegalArgumentException(
					"Expire time must be greater than 0");
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final CachedData data = transcoder.encode(value);
		byte[] keyBytes = ByteUtils.getBytes(key);
		byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlags()));
		byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
		byte[] dataLenBytes = ByteUtils.getBytes(String
				.valueOf(data.getData().length));
		byte[] casBytes = ByteUtils.getBytes(String.valueOf(cas));
		int size = cmd.length() + 1 + keyBytes.length + 1 + flagBytes.length
				+ 1 + expBytes.length + 1 + data.getData().length + 2
				* ByteUtils.CRLF.length + dataLenBytes.length;
		if (cmdType == Command.CommandType.CAS) {
			size += 1 + casBytes.length;
		}
		final IoBuffer buffer = this.byteBufferAllocator.allocate(size);
		if (cmdType == Command.CommandType.CAS) {
			ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
					dataLenBytes, casBytes);
		} else {
			ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
					dataLenBytes);
		}
		ByteUtils.setArguments(buffer, data.getData());
		buffer.flip();
		Command command = new Command(key, cmdType, latch);
		command.setIoBuffer(buffer);

		if (!sendCommand(command)) {
			throw new MemcachedException("send command fail");
		}
		latchWait(command, timeout, latch);
		buffer.free();
		if (command.getException() != null) {
			throw command.getException();
		}
		if (command.getResult() == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return (Boolean) command.getResult();
	}

	private void latchWait(Command cmd, long timeout, final CountDownLatch latch)
			throws InterruptedException, TimeoutException {
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			cmd.cancel();
			throw new TimeoutException("Timed out waiting for operation");
		}
	}
}
