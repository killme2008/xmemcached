package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.IOException;

import net.rubyeye.xmemcached.command.Command;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.TCPConnectorController;

public class XMemcachedClient {

	private static final long TIMEOUT = 1500;

	private TCPConnectorController connector;

	private Transcoder transcoder = new SerializingTranscoder();

	public XMemcachedClient(String server, int port) throws IOException {
		super();
		Configuration configuration = new Configuration();
		configuration.setTcpRecvBufferSize(16 * 1024);
		configuration.setSessionReadBufferSize(32 * 1024);
		configuration.setTcpNoDelay(true);
		this.connector = new TCPConnectorController(configuration);
		this.connector.setCodecFactory(new MemcachedCodecFactory());
		this.connector.setHandler(new MemcachedHandler(this.transcoder));
		this.connector.connect(new InetSocketAddress(server, port));
		this.connector.setSendBufferSize(8 * 1024);
		try {
			this.connector.awaitForConnected();
		} catch (InterruptedException e) {

		}
	}

	public Object get(String key) throws TimeoutException, InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		Command getCmd = new Command(key, Command.CommandType.GET_ONE, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				StringBuffer sb = new StringBuffer("get ");
				sb.append(this.getKey()).append(SPLIT);
				//System.out.println(sb.toString());
				return new ByteBuffer[] { ByteBuffer.wrap(sb.toString()
						.getBytes()) };
			}

		};

		this.connector.send(getCmd);
		if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Timed out waiting for operation");
		}
		if (getCmd.getThrowable() != null)
			throw getCmd.getThrowable();
		return getCmd.getResult();
	}

	static final int DEFAULT_FLAG = 512;

	public boolean set(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.SET,
				"set ");
	}

	public boolean add(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.ADD,
				"add ");
	}

	public boolean replace(final String key, final int exp, Object value)
			throws TimeoutException, InterruptedException {
		return sendStoreCommand(key, exp, value, Command.CommandType.REPLACE,
				"replace ");
	}

	private boolean sendStoreCommand(final String key, final int exp,
			Object value, Command.CommandType cmdType, final String cmd)
			throws InterruptedException, TimeoutException {
		final CountDownLatch latch = new CountDownLatch(1);
		final CachedData data = this.transcoder.encode(value);
		Command command = new Command(key, cmdType, latch) {
			@Override
			public ByteBuffer[] getCmd() {
				StringBuffer sb = new StringBuffer(cmd);
				sb.append(key).append(" ").append(data.getFlags()).append(" ")
						.append(exp).append(" ").append(data.getData().length)
						.append(SPLIT);
				ByteBuffer dataBuffer = ByteBuffer
						.allocate(data.getData().length + 2);
			//	System.out.println(sb.toString());
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
		if (command.getThrowable() != null)
			throw command.getThrowable();
		return (Boolean) command.getResult();
	}

	public static void main(String[] args) {
		try {
			XMemcachedClient client = new XMemcachedClient("192.168.222.100",
					11211);
			for (int i = 0; i < 100000; i++) {
				assert (client.set("hello" + i,0,i));

			}
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {
				assert ((Integer) client.get("hello" + i) == i);
				
			}
			System.out.println(System.currentTimeMillis() - start);
			System.out.println(client.get("hello"));
			// long start = System.currentTimeMillis();
			// for (int i = 0; i < 10000; i++)
			// assert (((HashMap) client.get("test")) instanceof HashMap);
			// System.out.println(System.currentTimeMillis() - start);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
