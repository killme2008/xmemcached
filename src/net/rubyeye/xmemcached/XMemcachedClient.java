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

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.Session;

public class XMemcachedClient {

	public static final int CONNECT_TIMEOUT = 3000;
	private static final int TCP_SEND_BUFF_SIZE = 16 * 1024;
	private static final boolean TCP_NO_DELAY = false;
	private static final int READ_BUFF_SIZE = 32 * 1024;
	private static final int TCP_RECV_BUFF_SIZE = 16 * 1024;
	private static final long TIMEOUT = 1000;
	protected static final Log log = LogFactory.getLog(XMemcachedClient.class);

	protected MemcachedSessionLocator sessionLocator;
	/**
	 * ���Ե�ƽ��ֵ�����ʵ��������
	 */
	private volatile boolean shutdown;

	public void setGetsMergeFactor(int mergeFactor) {
		this.connector.setGetsMergeFactor(mergeFactor);
	}

	public MemcachedConnector getConnector() {
		return connector;
	}

	public void setOptimiezeGet(boolean optimiezeGet) {
		this.connector.setOptimiezeGet(optimiezeGet);
	}

	public void setOptimiezeSet(boolean optimiezeSet) {
		this.connector.setoptimizeSet(optimiezeSet);
	}

	public boolean isShutdown() {
		return shutdown;
	}

	private void sendCommand(Command cmd) throws InterruptedException {
		if (this.shutdown) {
			throw new IllegalStateException();
		}
		connector.send(cmd);
	}

	private MemcachedConnector connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	private MemcachedHandler memcachedHandler;

	public XMemcachedClient(String server, int port) throws IOException {
		super();
		checkServerPort(server, port);
		buildConnector(new ArrayMemcachedSessionLocator());
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

	public void addServer(String server, int port) throws IOException {
		checkServerPort(server, port);
		connect(new InetSocketAddress(server, port));
	}

	public void addServer(InetSocketAddress inetSocketAddress) {
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		connect(inetSocketAddress);
	}

	private void connect(InetSocketAddress inetSocketAddress) {
		Future<Boolean> future = null;
		try {
			future = this.connector.connect(inetSocketAddress);

			if (!future.get(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS))
				log.error("connect to " + inetSocketAddress.getHostName() + ":"
						+ inetSocketAddress.getPort() + " fail");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			if (future != null)
				future.cancel(true);
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " error", e);
		} catch (TimeoutException e) {
			if (future != null)
				future.cancel(true);
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " timeout");
		} catch (Exception e) {
			if (future != null)
				future.cancel(true);
			log.error("connect to " + inetSocketAddress.getHostName() + ":"
					+ inetSocketAddress.getPort() + " error", e);
		}

	}

	private void startConnector() throws IOException {
		if (this.shutdown) {
			this.connector.start();
			this.shutdown = false;
		}
	}

	private void buildConnector(MemcachedSessionLocator locator) {
		if (locator == null)
			throw new IllegalArgumentException();
		this.sessionLocator = locator;
		Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(READ_BUFF_SIZE);
		configuration.setTcpNoDelay(TCP_NO_DELAY);
		configuration.setReadThreadCount(0);
		this.shutdown = true;
		this.connector = new MemcachedConnector(configuration, sessionLocator);
		this.connector.setSendBufferSize(TCP_SEND_BUFF_SIZE);
		this.transcoder = new SerializingTranscoder();
		this.memcachedHandler = new MemcachedHandler(this.transcoder, this);
		this.connector.setHandler(memcachedHandler);
		this.connector.setMemcachedProtocolHandler(memcachedHandler);
	}

	public XMemcachedClient(InetSocketAddress inetSocketAddress)
			throws IOException {
		super();
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		buildConnector(new ArrayMemcachedSessionLocator());
		startConnector();
		connect(inetSocketAddress);
	}

	public XMemcachedClient() throws IOException {
		super();
		buildConnector(new ArrayMemcachedSessionLocator());
		startConnector();
	}

	public XMemcachedClient(MemcachedSessionLocator locator) throws IOException {
		super();
		buildConnector(locator);
		startConnector();
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
		final ByteBuffer buffer = ByteBuffer.allocate(cmdBytes.length
				+ ByteUtils.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
		buffer.flip();
		Command getCmd = new Command(key, cmdType, latch) {
			@Override
			public ByteBuffer getByteBuffer() {

				setByteBuffer(buffer);
				return buffer;
			}
		};
		sendCommand(getCmd);
		latchWait(timeout, latch);
		if (getCmd.getException() != null) {
			throw getCmd.getException();
		}
		return getCmd.getResult();
	}

	public Object get(final String key) throws TimeoutException,
			InterruptedException {
		return get(key, TIMEOUT, ByteUtils.GET, Command.CommandType.GET_ONE);
	}

	public GetsResponse gets(final String key) throws TimeoutException,
			InterruptedException {
		return (GetsResponse) get(key, TIMEOUT, ByteUtils.GETS,
				Command.CommandType.GETS_ONE);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> get(Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException {
		// 超时时间加倍
		long lazy = keyCollections.size() / 1000 > 0 ? (keyCollections.size() / 1000)
				: 1;
		lazy = lazy > 5 ? 5 : lazy; // 最高5秒
		return (Map<String, Object>) get(keyCollections, lazy * TIMEOUT,
				ByteUtils.GET, Command.CommandType.GET_MANY);
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
		return (Map<String, GetsResponse>) get(keyCollections, lazy * TIMEOUT,
				ByteUtils.GETS, Command.CommandType.GETS_MANY);
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
		lazy = lazy > 5 ? 5 : lazy; // 最高5秒
		latchWait(timeout*lazy, latch);
		for (Command getCmd : commands) {
			if (getCmd.getException() != null)
				throw getCmd.getException();
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
			} else
				catalogMap.get(index).add(key);
		}

		Collection<List<String>> catalogKeys = catalogMap.values();
		return catalogKeys;
	}

	@SuppressWarnings("unchecked")
	private Command sendGetManyCommand(List<String> keys, CountDownLatch latch,
			Map result, byte[] cmdBytes, Command.CommandType cmdType)
			throws InterruptedException, TimeoutException {
		Command getCmd = new Command(keys.get(0), cmdType, latch);
		getCmd.setResult(result); // 共用一个result map
		StringBuilder sb = new StringBuilder(keys.size() * 5);
		for (String tmpKey : keys) {
			ByteUtils.checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		byte[] keyBytes = ByteUtils.getBytes(sb.toString());
		final ByteBuffer buffer = ByteBuffer.allocate(cmdBytes.length
				+ ByteUtils.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
		buffer.flip();
		getCmd.setByteBuffer(buffer);
		sendCommand(getCmd);
		return getCmd;
	}

	public boolean set(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", TIMEOUT, -1);
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
				"add", TIMEOUT, -1);
	}

	public boolean add(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"add", timeout, -1);
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"replace", TIMEOUT, -1);
	}

	public boolean replace(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"replace", timeout, -1);
	}

	public boolean append(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"append", TIMEOUT, -1);
	}

	public boolean append(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"append", timeout, -1);
	}

	public boolean prepend(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"prepend", TIMEOUT, -1);
	}

	public boolean prepend(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
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
				"cas", TIMEOUT, cas);
	}

	public boolean cas(final String key, final int exp, CASOperation operation)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		if (operation == null)
			throw new IllegalArgumentException("CASOperation could not be null");
		if (operation.getMaxTries() < 0)
			throw new IllegalArgumentException("max tries less than 0");
		int tryCount = 0;
		GetsResponse result = gets(key);
		if (result == null)
			throw new NullPointerException("could not found the key " + key);
		while (tryCount < operation.getMaxTries()
				&& result != null
				&& !sendStoreCommand(key, exp, operation.getNewValue(result
						.getCas(), result.getValue()), Command.CommandType.CAS,
						"cas", TIMEOUT, result.getCas())) {
			tryCount++;
			result = gets(key);
			if (result == null)
				throw new NullPointerException("could not found the value for Key=" + key);
			if (tryCount >= operation.getMaxTries())
				throw new TimeoutException("CAS try times is greater than max");
		}
		return true;
	}

	public boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] keyBytes = ByteUtils.getBytes(key);
		byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
		final ByteBuffer buffer = ByteBuffer.allocate(ByteUtils.DELETE.length
				+ 2 + keyBytes.length + timeBytes.length
				+ ByteUtils.CRLF.length);
		ByteUtils.setArguments(buffer, ByteUtils.DELETE, keyBytes, timeBytes);
		buffer.flip();
		Command command = new Command(key, Command.CommandType.DELETE, latch) {

			@Override
			public ByteBuffer getByteBuffer() {
				setByteBuffer(buffer);
				return buffer;
			}
		};

		sendCommand(command);
		latchWait(TIMEOUT, latch);
		if (command.getException() != null) {
			throw command.getException();
		}
		return (Boolean) command.getResult();
	}

	public String version() throws TimeoutException, InterruptedException,
			MemcachedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final ByteBuffer buffer = ByteBuffer.wrap("version\r\n".getBytes());
		Command command = new Command("version", Command.CommandType.VERSION,
				latch) {

			@Override
			public ByteBuffer getByteBuffer() {
				setByteBuffer(buffer);
				return buffer;
			}
		};

		sendCommand(command);
		latchWait(TIMEOUT, latch);
		if (command.getException() != null) {
			throw command.getException();
		}
		return (String) command.getResult();
	}

	public int incr(final String key, final int num) throws TimeoutException,
			InterruptedException {
		ByteUtils.checkKey(key);
		return sendIncrOrDecrCommand(key, num, Command.CommandType.INCR, "incr");
	}

	public int decr(final String key, final int num) throws TimeoutException,
			InterruptedException {
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
		final ByteBuffer buffer = ByteBuffer.allocate(cmd.length() + 2
				+ key.length() + numBytes.length + ByteUtils.CRLF.length);
		ByteUtils.setArguments(buffer, cmdBytes, keyBytes, numBytes);
		buffer.flip();
		Command command = new Command(key, cmdType, latch) {

			@Override
			public ByteBuffer getByteBuffer() {
				setByteBuffer(buffer);
				return buffer;
			}
		};

		sendCommand(command);
		latchWait(TIMEOUT, latch);
		if (command.getException() != null) {
			throw command.getException();
		}
		if (command.getResult() instanceof Boolean
				&& !((Boolean) command.getResult())) {
			return -1;
		} else {
			return (Integer) command.getResult();
		}
	}

	public boolean delete(final String key) throws TimeoutException,
			InterruptedException {
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
		if (value == null)
			throw new IllegalArgumentException("value could not be null");
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
		if (cmdType.equals(Command.CommandType.CAS)) {
			size += 1 + casBytes.length;
		}
		final ByteBuffer buffer = ByteBuffer.allocate(size);
		if (cmdType.equals(Command.CommandType.CAS)) {
			ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
					dataLenBytes, casBytes);
		} else
			ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
					dataLenBytes);
		ByteUtils.setArguments(buffer, data.getData());
		buffer.flip();
		Command command = new Command(key, cmdType, latch) {

			@Override
			public ByteBuffer getByteBuffer() {

				setByteBuffer(buffer);
				return buffer;
			}
		};

		sendCommand(command);
		latchWait(timeout, latch);
		if (command.getException() != null) {
			throw command.getException();
		}
		return (Boolean) command.getResult();
	}

	private void latchWait(long timeout, final CountDownLatch latch)
			throws InterruptedException, TimeoutException {
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
	}
}
