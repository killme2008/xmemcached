package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
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

public class XMemcachedClient {

	private static final int CONNECT_TIMEOUT = 3000;
	private static final int TCP_SEND_BUFF_SIZE = 16 * 1024;
	private static final boolean TCP_NO_DELAY = false;
	private static final int READ_BUFF_SIZE = 32 * 1024;
	private static final int TCP_RECV_BUFF_SIZE = 16 * 1024;
	private static final long TIMEOUT = 1000;
	protected static final Log log = LogFactory.getLog(XMemcachedClient.class);
	/**
	 * ���Ե�ƽ��ֵ�����ʵ��������
	 */
	private volatile boolean shutdown;

	public int getGetsMergeFactor() {
		return this.connector.getSession().getsMergeFactor;
	}

	public void setGetsMergeFactor(int mergeFactor) {
		this.connector.getSession().getsMergeFactor = mergeFactor;
	}

	public MemcachedConnector getConnector() {
		return connector;
	}

	public boolean isOptimiezeGet() {
		return this.connector.getSession().optimiezeGet;
	}

	public void setOptimiezeGet(boolean optimiezeGet) {
		this.connector.getSession().optimiezeGet = optimiezeGet;
	}

	public boolean isOptimiezeSet() {
		return this.connector.getSession().optimiezeSet;
	}

	public void setOptimiezeSet(boolean optimiezeSet) {
		this.connector.getSession().optimiezeSet = optimiezeSet;
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

	public void addServer(InetSocketAddress inetSocketAddress)
			throws IOException {
		if (inetSocketAddress == null) {
			throw new IllegalArgumentException();
		}
		connect(inetSocketAddress);
	}

	private void connect(InetSocketAddress inetSocketAddress)
			throws IOException {
		Future<Boolean> future = this.connector.connect(inetSocketAddress);
		try {
			if (!future.get(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS))
				throw new IOException("connect to "
						+ inetSocketAddress.getHostName() + ":"
						+ inetSocketAddress.getPort() + " fail");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			future.cancel(true);
			throw new IOException(e);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new IOException(e);
		} catch (Exception e) {
			future.cancel(true);
			throw new IOException(e);
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
		Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(READ_BUFF_SIZE);
		configuration.setTcpNoDelay(TCP_NO_DELAY);
		configuration.setReadThreadCount(0);
		this.shutdown = true;
		this.connector = new MemcachedConnector(configuration, locator);
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
		ByteUtils.checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		byte[] keyBytes = ByteUtils.getBytes(key);
		final ByteBuffer buffer = ByteBuffer.allocate(ByteUtils.GET.length
				+ ByteUtils.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, ByteUtils.GET, keyBytes);
		buffer.flip();
		Command getCmd = new Command(key, Command.CommandType.GET_ONE, latch) {

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
		return get(key, TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> get(Collection<String> keys)
			throws TimeoutException, InterruptedException, MemcachedException {
		if (keys == null || keys.size() == 0) {
			return null;
		}
		final CountDownLatch latch = new CountDownLatch(1);
		StringBuilder sb = new StringBuilder(keys.size() * 5);
		for (String tmpKey : keys) {
			ByteUtils.checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		final String key = sb.toString();
		byte[] keyBytes = ByteUtils.getBytes(key);
		final ByteBuffer buffer = ByteBuffer.allocate(ByteUtils.GET.length
				+ ByteUtils.CRLF.length + 1 + keyBytes.length);
		ByteUtils.setArguments(buffer, ByteUtils.GET, keyBytes);
		buffer.flip();

		Command getCmd = new Command(key.substring(0, key.length() - 1),
				Command.CommandType.GET_MANY, latch) {

			@Override
			public ByteBuffer getByteBuffer() {
				setByteBuffer(buffer);
				return buffer;
			}
		};
		long lazy = keys.size() / 1000 > 0 ? (keys.size() / 1000) : 1;
		sendCommand(getCmd);
		latchWait(lazy, latch);
		if (getCmd.getException() != null) {
			throw getCmd.getException();
		}
		return (Map<String, Object>) getCmd.getResult();
	}

	public boolean set(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", TIMEOUT);
	}

	public boolean set(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", timeout);
	}

	public boolean add(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.ADD,
				"add", TIMEOUT);
	}

	public boolean add(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"add", timeout);
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException, MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"replace", TIMEOUT);
	}

	public boolean replace(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException {
		ByteUtils.checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"replace", timeout);
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

	public Transcoder getTranscoder() {
		return transcoder;
	}

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
			long timeout) throws InterruptedException, TimeoutException,
			MemcachedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final CachedData data = transcoder.encode(value);
		byte[] keyBytes = ByteUtils.getBytes(key);
		byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlags()));
		byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
		byte[] dataLenBytes = ByteUtils.getBytes(String
				.valueOf(data.getData().length));
		final ByteBuffer buffer = ByteBuffer.allocate(cmd.length() + 1
				+ keyBytes.length + 1 + flagBytes.length + 1 + expBytes.length
				+ 1 + data.getData().length + 2 * ByteUtils.CRLF.length
				+ dataLenBytes.length);
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
