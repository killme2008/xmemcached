package net.rubyeye.xmemcached.uds;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.code.juds.UnixDomainSocket;
import com.google.code.juds.UnixDomainSocketClient;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.ControllerStateListener;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;
import com.google.code.yanf4j.core.impl.FutureImpl;

import net.rubyeye.xmemcached.Connector;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.test.unittest.impl.SessionLocatorTest;

public class UDSocketConnector implements Connector {

	public void addStateListener(ControllerStateListener listener) {
		// TODO Auto-generated method stub

	}

	public void addToWatingQueue(ReconnectRequest request) {
		// TODO Auto-generated method stub

	}

	private UDSocketSession session;

	public Future<Boolean> connect(InetSocketAddress address, int weight)
			throws IOException {
		UDSocketAddress udsocketAddress = (UDSocketAddress) address;
		for (int i = 0; i < 3; i++) {
			UnixDomainSocketClient client = new UnixDomainSocketClient(
					udsocketAddress.getPath(), UnixDomainSocket.SOCK_STREAM);

			sessionList.add(new UDSocketSession(udsocketAddress.getPath(),
					client, new SimpleBufferAllocator()));
		}
		FutureImpl<Boolean> result = new FutureImpl<Boolean>();
		result.setResult(true);
		return result;
	}

	public Queue<Session> getSessionByAddress(InetSocketAddress address) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Session> getSessionListBySocketAddress(InetSocketAddress address) {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<Session> getSessionSet() {
		return new HashSet<Session>();
	}

	public void removeReconnectRequest(InetSocketAddress address) {
		// TODO Auto-generated method stub

	}

	public void removeSession(Session session) {
		// TODO Auto-generated method stub

	}

	public void removeStateListener(ControllerStateListener listener) {
		// TODO Auto-generated method stub

	}

	List<UDSocketSession> sessionList = new ArrayList<UDSocketSession>();
	final Random random = new Random(3);

	public void send(Command packet) throws MemcachedException {
		int i = random.nextInt(3);
		final UDSocketSession session = sessionList.get(i);
		synchronized (session) {
			session.write(packet);
			try {
				session.readFromInputStream(packet);
			} catch (IOException e) {
				throw new MemcachedException(e);
			}
		}

	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		// TODO Auto-generated method stub

	}

	public void setCodecFactory(CodecFactory codecFactory) {
		// TODO Auto-generated method stub

	}

	public void setConnectionPoolSize(int connectionPoolSize) {
		// TODO Auto-generated method stub

	}

	public void setHandler(Handler handler) {
		// TODO Auto-generated method stub

	}

	public void setHealSessionInterval(long interval) {
		// TODO Auto-generated method stub

	}

	public void setMergeFactor(int factor) {
		// TODO Auto-generated method stub

	}

	public void setOptimizeGet(boolean optimizeGet) {
		// TODO Auto-generated method stub

	}

	public void setOptimizeMergeBuffer(boolean optimiezeMergeBuffer) {
		// TODO Auto-generated method stub

	}

	public void setSessionTimeout(long timeout) {
		// TODO Auto-generated method stub

	}

	public void setSocketOptions(Map<SocketOption, Object> options) {
		// TODO Auto-generated method stub

	}

	public void start() throws IOException {
		// TODO Auto-generated method stub

	}

	public void stop() throws IOException {
		// TODO Auto-generated method stub

	}

	public void updateSessions() {
		// TODO Auto-generated method stub

	}

}
