package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.IOException;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.TCPConnectorController;

public class XMemcachedClient {

	private static final int TCP_SEND_BUFF_SIZE = 8 * 1024;

	private static final boolean TCP_NO_DELAY = true;

	private static final int READ_BUFF_SIZE = 32 * 1024;

	private static final int TCP_RECV_BUFF_SIZE = 16 * 1024;

	private static final long TIMEOUT = 1000;

	private TCPConnectorController connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;

	public XMemcachedClient(String server, int port) throws IOException {
		super();
		buildConnector();
		connect(new InetSocketAddress(server, port));
	}

	private void connect(InetSocketAddress inetSocketAddress)
			throws IOException {
		this.connector.connect(inetSocketAddress);
		try {
			this.connector.awaitForConnected();
		} catch (InterruptedException e) {

		}
	}

	private void buildConnector() {
		Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(READ_BUFF_SIZE);
		configuration.setTcpNoDelay(TCP_NO_DELAY);
		this.connector = new TCPConnectorController(configuration);
		this.connector.setSendBufferSize(TCP_SEND_BUFF_SIZE);
		this.connector.setCodecFactory(new MemcachedCodecFactory());
		this.transcoder = new SerializingTranscoder();
		this.connector.setHandler(new MemcachedHandler(this.transcoder));
	}

	public XMemcachedClient(InetSocketAddress inetSocketAddress)
			throws IOException {
		super();
		buildConnector();
		connect(inetSocketAddress);
	}

	private void checkKey(String key) {
		if (key == null || key.length() == 0)
			throw new MemcachedException("wrong key,key can not be blank");
		if (key.indexOf(" ") >= 0)
			throw new MemcachedException(
					"wrong key,key can not have space character");
		if (key.indexOf("\r\n") >= 0)
			throw new MemcachedException(
					"wrong key,key can not have \\r\\n character");
	}

	public Object get(final String key) throws TimeoutException,
			InterruptedException {
		checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		Command getCmd = new Command(key, Command.CommandType.GET_ONE, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				StringBuffer sb = new StringBuffer(6 + key.length());
				sb.append("get ").append(this.getKey()).append(SPLIT);
				return new ByteBuffer[] { ByteBuffer.wrap(sb.toString()
						.getBytes()) };
			}

		};

		this.connector.send(getCmd);
		if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (getCmd.getException() != null)
			throw getCmd.getException();
		return getCmd.getResult();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> get(Collection<String> keys)
			throws TimeoutException, InterruptedException {
		if (keys == null || keys.size() == 0)
			return null;
		final CountDownLatch latch = new CountDownLatch(1);
		StringBuffer sb = new StringBuffer(keys.size() * 5);
		for (String tmpKey : keys) {
			checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		String key = sb.toString();
		Command getCmd = new Command(key.substring(0, key.length() - 1),
				Command.CommandType.GET_MANY, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				StringBuffer sb = new StringBuffer("get ");
				sb.append(this.getKey()).append(SPLIT);
				return new ByteBuffer[] { ByteBuffer.wrap(sb.toString()
						.getBytes()) };
			}

		};
		long lazy = keys.size() / 1000 > 0 ? (keys.size() / 1000) : 1;
		this.connector.send(getCmd);
		if (!latch.await(TIMEOUT * lazy, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (getCmd.getException() != null)
			throw getCmd.getException();
		return (Map<String, Object>) getCmd.getResult();
	}

	public boolean set(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set ");
	}

	public boolean add(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.ADD,
				"add ");
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"replace ");
	}

	public boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(key, Command.CommandType.DELETE, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				StringBuffer sb = new StringBuffer(13 + key.length());
				sb.append("delete ").append(key).append(" ").append(time)
						.append(SPLIT);
				return new ByteBuffer[] { ByteBuffer.wrap(sb.toString()
						.getBytes()) };
			}

		};

		this.connector.send(command);
		if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (command.getException() != null)
			throw command.getException();
		return (Boolean) command.getResult();
	}

	public String version() throws TimeoutException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(Command.CommandType.VERSION, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				return new ByteBuffer[] { ByteBuffer.wrap("version\r\n"
						.toString().getBytes()) };
			}

		};

		this.connector.send(command);
		if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (command.getException() != null)
			throw command.getException();
		return (String) command.getResult();
	}

	public int incr(final String key, final int num) throws TimeoutException,
			InterruptedException {
		checkKey(key);
		return sendIncrOrDecrCommand(key, num, Command.CommandType.INCR,
				"incr ");
	}

	public int decr(final String key, final int num) throws TimeoutException,
			InterruptedException {
		checkKey(key);
		return sendIncrOrDecrCommand(key, num, Command.CommandType.DECR,
				"decr ");
	}

	public void shutdown() throws IOException {
		this.connector.stop();
	}

	private int sendIncrOrDecrCommand(final String key, final int num,
			Command.CommandType cmdType, final String cmd)
			throws InterruptedException, TimeoutException {
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(key, cmdType, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				StringBuffer sb = new StringBuffer(cmd.length() + 7);
				sb.append(cmd).append(key).append(" ").append(num)
						.append(SPLIT);
				return new ByteBuffer[] { ByteBuffer.wrap(sb.toString()
						.getBytes()) };
			}

		};

		this.connector.send(command);
		if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (command.getException() != null)
			throw command.getException();
		if (command.getResult() instanceof Boolean
				&& !((Boolean) command.getResult()))
			return -1;
		else
			return (Integer) command.getResult();
	}

	public boolean delete(final String key) throws TimeoutException,
			InterruptedException {
		return delete(key, 0);
	}

	@SuppressWarnings("unchecked")
	private boolean sendStoreCommand(final String key, final int exp,
			final Object value, Command.CommandType cmdType, final String cmd)
			throws InterruptedException, TimeoutException {
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(key, cmdType, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				final CachedData data = transcoder.encode(value);
				StringBuffer sb = new StringBuffer(cmd.length() + 16
						+ data.getData().length);
				sb.append(cmd).append(key).append(" ").append(data.getFlags())
						.append(" ").append(exp).append(" ").append(
								data.getData().length).append(SPLIT);
				ByteBuffer dataBuffer = ByteBuffer
						.allocate(data.getData().length + 2);
				dataBuffer.put(data.getData());
				dataBuffer.put(SPLIT.getBytes());
				dataBuffer.flip();
				return new ByteBuffer[] {
						ByteBuffer.wrap(sb.toString().getBytes()), dataBuffer };
			}

		};

		this.connector.send(command);
		if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (command.getException() != null)
			throw command.getException();
		return (Boolean) command.getResult();
	}

}
