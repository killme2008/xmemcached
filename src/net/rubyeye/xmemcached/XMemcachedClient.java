package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.TCPConnectorController;
import com.google.code.yanf4j.util.Queue;
import com.google.code.yanf4j.util.SimpleQueue;

public class XMemcachedClient {

	private static final int TCP_SEND_BUFF_SIZE = 8 * 1024;

	private static final boolean TCP_NO_DELAY = true;

	private static final int READ_BUFF_SIZE = 32 * 1024;

	private static final int TCP_RECV_BUFF_SIZE = 16 * 1024;

	private static final long TIMEOUT = 1000;
	protected static final Log log = LogFactory.getLog(XMemcachedClient.class);
	/**
	 * 测试的平均值，根据实际情况调整
	 */
	private int mergeFactor = 60;

	private volatile boolean shutdown;

	protected Queue<Command> commands = new SimpleQueue<Command>();

	CommandSender commandSender;

	public int getMergeFactor() {
		return mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		this.mergeFactor = mergeFactor;
	}

	public TCPConnectorController getConnector() {
		return connector;
	}

	public boolean isOptimiezeGet() {
		return optimiezeGet;
	}

	public void setOptimiezeGet(boolean optimiezeGet) {
		this.optimiezeGet = optimiezeGet;
	}

	public boolean isShutdown() {
		return shutdown;
	}

	private void sendCommand(Command cmd) throws InterruptedException {
		if (this.shutdown)
			throw new IllegalStateException();
		commands.push(cmd);
	}

	private boolean optimiezeGet = true;

	class CommandSender extends Thread {

		// 统计数据，平均merge因子
		int total = 0;
		int count = 0;

		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Command currentCmd = commands.pop();
					if (currentCmd.getCommandType().equals(
							Command.CommandType.GET_ONE)) {
						final List<Command> mergeCommands = new ArrayList<Command>();
						mergeCommands.add(currentCmd);
						if (optimiezeGet)
							optimizeGet(currentCmd, mergeCommands);
						else
							connector.send(currentCmd);
					} else
						connector.send(currentCmd);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					log.error("send command error", e);
				}
			}
		}

		private void optimizeGet(Command currentCmd,
				final List<Command> mergeCommands) throws InterruptedException {
			int i = 1;
			final StringBuilder key = new StringBuilder();
			key.append((String) currentCmd.getKey());
			while (i < mergeFactor) {
				Command nextCmd = commands.peek();
				if (nextCmd == null)
					break;
				if (nextCmd.getCommandType()
						.equals(Command.CommandType.GET_ONE)) {
					mergeCommands.add(commands.pop());
					key.append(" ").append((String) nextCmd.getKey());
					i++;
				} else
					break;
			}
			// 没有可以合并的，直接发送
			if (i == 1)
				connector.send(currentCmd);
			else {
				// 统计
				currentCmd.setMergetCount(mergeCommands.size());
				count++;
				total +=currentCmd.getMergetCount();
				log.debug("merge "+currentCmd.getMergetCount()+" get operations,current average merge factor is"+total/count);
				// 发送合并get操作
				connector.send(new Command(key.toString(),
						Command.CommandType.GET_ONE, null) {
					public List<Command> getMergeCommands() {
						return mergeCommands;
					}

					public ByteBuffer getCmd() {
						byte[] keyBytes = ByteUtils.getBytes(key.toString());
						ByteBuffer buffer = ByteBuffer.allocate(GET.length
								+ CRLF.length + 1 + keyBytes.length);
						setArguments(buffer, GET, keyBytes);
						buffer.flip();
						return buffer;
					}
				});
			}
		}
	}

	private TCPConnectorController connector;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;

	private InetSocketAddress serverAddress;

	public XMemcachedClient(String server, int port) throws IOException {
		super();
		if (server == null || server.length() == 0)
			throw new IllegalArgumentException();
		if (port <= 0)
			throw new IllegalArgumentException();
		this.serverAddress = new InetSocketAddress(server, port);
		buildConnector();
		connect();
	}

	private void connect() throws IOException {
		// 启动发送线程
		this.commandSender.start();
		// 连接
		this.connector.connect(this.serverAddress);
		try {
			this.connector.awaitForConnected();
		} catch (InterruptedException e) {

		}
		this.shutdown = false;
	}

	private void buildConnector() {
		Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(TCP_RECV_BUFF_SIZE);
		configuration.setSessionReadBufferSize(READ_BUFF_SIZE);
		configuration.setTcpNoDelay(TCP_NO_DELAY);
		this.shutdown = true;
		this.connector = new TCPConnectorController(configuration);
		this.connector.setSendBufferSize(TCP_SEND_BUFF_SIZE);
		this.connector.setCodecFactory(new MemcachedCodecFactory());
		this.transcoder = new SerializingTranscoder();
		this.commandSender = new CommandSender();
		this.connector.setHandler(new MemcachedHandler(this.transcoder,
				this.connector));
	}

	public XMemcachedClient(InetSocketAddress inetSocketAddress)
			throws IOException {
		super();
		if (inetSocketAddress == null)
			throw new IllegalArgumentException();
		this.serverAddress = inetSocketAddress;
		buildConnector();
		connect();
	}

	int MAX_KEY_LENGTH = 250;

	private void checkKey(String key) {
		byte[] keyBytes = ByteUtils.getBytes(key);
		if (keyBytes.length > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for (byte b : keyBytes) {
			if (b == ' ' || b == '\n' || b == '\r' || b == 0) {
				throw new IllegalArgumentException(
						"Key contains invalid characters:  ``" + key + "''");
			}
		}
	}

	static final byte[] CRLF = { '\r', '\n' };
	static final byte[] GET = { 'g', 'e', 't' };
	static final byte[] DELETE = { 'd', 'e', 'l', 'e', 't', 'e' };
	static final byte SPACE = ' ';

	protected final void setArguments(ByteBuffer bb, Object... args) {
		boolean wasFirst = true;
		for (Object o : args) {
			if (wasFirst) {
				wasFirst = false;
			} else {
				bb.put(SPACE);
			}
			if (o instanceof byte[])
				bb.put((byte[]) o);
			else
				bb.put(ByteUtils.getBytes(String.valueOf(o)));
		}
		bb.put(CRLF);
	}

	public Object get(final String key, long timeout) throws TimeoutException,
			InterruptedException {
		checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		Command getCmd = new Command(key, Command.CommandType.GET_ONE, latch) {
			@Override
			public ByteBuffer getCmd() {
				byte[] keyBytes = ByteUtils.getBytes(key);
				ByteBuffer buffer = ByteBuffer.allocate(GET.length
						+ CRLF.length + 1 + keyBytes.length);
				setArguments(buffer, GET, keyBytes);
				buffer.flip();
				return buffer;
			}

		};
		sendCommand(getCmd);
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (getCmd.getException() != null)
			throw getCmd.getException();
		return getCmd.getResult();
	}

	public Object get(final String key) throws TimeoutException,
			InterruptedException {
		return get(key, TIMEOUT);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> get(Collection<String> keys)
			throws TimeoutException, InterruptedException {
		if (keys == null || keys.size() == 0)
			return null;
		final CountDownLatch latch = new CountDownLatch(1);
		StringBuilder sb = new StringBuilder(keys.size() * 5);
		for (String tmpKey : keys) {
			checkKey(tmpKey);
			sb.append(tmpKey).append(" ");
		}
		final String key = sb.toString();
		Command getCmd = new Command(key.substring(0, key.length() - 1),
				Command.CommandType.GET_MANY, latch) {
			@Override
			public ByteBuffer getCmd() {
				byte[] keyBytes = ByteUtils.getBytes(key);
				ByteBuffer buffer = ByteBuffer.allocate(GET.length
						+ CRLF.length + 1 + keyBytes.length);
				setArguments(buffer, GET, keyBytes);
				buffer.flip();
				return buffer;
			}

		};
		long lazy = keys.size() / 1000 > 0 ? (keys.size() / 1000) : 1;
		sendCommand(getCmd);
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
				"set", TIMEOUT);
	}

	public boolean set(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set", timeout);
	}

	public boolean add(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.ADD,
				"add", TIMEOUT);
	}

	public boolean add(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"add", timeout);
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"replace", TIMEOUT);
	}

	public boolean replace(final String key, final int exp, Object value,
			long timeout) throws TimeoutException, InterruptedException {
		checkKey(key);
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"replace", timeout);
	}

	public boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException {
		checkKey(key);
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(key, Command.CommandType.DELETE, latch) {
			@Override
			public ByteBuffer getCmd() {
				byte[] keyBytes = ByteUtils.getBytes(key);
				byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
				ByteBuffer buffer = ByteBuffer.allocate(DELETE.length + 2
						+ keyBytes.length + timeBytes.length + CRLF.length);
				setArguments(buffer, DELETE, keyBytes, timeBytes);
				buffer.flip();
				return buffer;
			}

		};

		sendCommand(command);
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
			public ByteBuffer getCmd() {
				return ByteBuffer.wrap("version\r\n".getBytes());
			}

		};

		sendCommand(command);
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
		return sendIncrOrDecrCommand(key, num, Command.CommandType.INCR, "incr");
	}

	public int decr(final String key, final int num) throws TimeoutException,
			InterruptedException {
		checkKey(key);
		return sendIncrOrDecrCommand(key, num, Command.CommandType.DECR, "decr");
	}

	public void shutdown() throws IOException {
		if (this.shutdown)
			return;
		this.shutdown = true;
		this.connector.stop();
		this.commandSender.interrupt();
		while (this.commandSender.isAlive())
			try {
				this.commandSender.join();
			} catch (InterruptedException e) {

			}
	}

	private int sendIncrOrDecrCommand(final String key, final int num,
			Command.CommandType cmdType, final String cmd)
			throws InterruptedException, TimeoutException {
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(key, cmdType, latch) {
			@Override
			public ByteBuffer getCmd() {
				byte[] numBytes = ByteUtils.getBytes(String.valueOf(num));
				byte[] cmdBytes = ByteUtils.getBytes(cmd);
				byte[] keyBytes = ByteUtils.getBytes(key);
				ByteBuffer buffer = ByteBuffer.allocate(cmd.length() + 2
						+ key.length() + numBytes.length + CRLF.length);
				setArguments(buffer, cmdBytes, keyBytes, numBytes);
				buffer.flip();
				return buffer;
			}

		};

		sendCommand(command);
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
			final Object value, Command.CommandType cmdType, final String cmd,
			long timeout) throws InterruptedException, TimeoutException {
		final CountDownLatch latch = new CountDownLatch(1);
		Command command = new Command(key, cmdType, latch) {
			@Override
			public ByteBuffer getCmd() {
				final CachedData data = transcoder.encode(value);
				byte[] keyBytes = ByteUtils.getBytes(key);
				byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data
						.getFlags()));
				byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
				byte[] dataLenBytes = ByteUtils.getBytes(String.valueOf(data
						.getData().length));
				ByteBuffer buffer = ByteBuffer.allocate(cmd.length() + 1
						+ keyBytes.length + 1 + flagBytes.length + 1
						+ expBytes.length + 1 + data.getData().length + 2
						* CRLF.length + dataLenBytes.length);
				setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
						dataLenBytes);
				setArguments(buffer, data.getData());
				buffer.flip();
				return buffer;
			}

		};

		sendCommand(command);
		if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (command.getException() != null)
			throw command.getException();
		return (Boolean) command.getResult();
	}

}
