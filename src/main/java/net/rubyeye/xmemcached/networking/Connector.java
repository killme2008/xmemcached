package net.rubyeye.xmemcached.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Future;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;

import com.google.code.yanf4j.core.Controller;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;

public interface Connector extends Controller {
	public void setOptimizeMergeBuffer(boolean optimiezeMergeBuffer);

	public void setMergeFactor(int factor);

	public void setOptimizeGet(boolean optimizeGet);

	public void removeSession(Session session);

	public Queue<Session> getSessionByAddress(InetSocketAddress address);

	public Set<Session> getSessionSet();

	public void setHealSessionInterval(long interval);

	public void send(Command packet) throws MemcachedException;

	public void setConnectionPoolSize(int connectionPoolSize);

	public void setBufferAllocator(BufferAllocator bufferAllocator);

	public void removeReconnectRequest(InetSocketAddress address);

	public void addToWatingQueue(ReconnectRequest request);

	public void setSocketOptions(Map<SocketOption, Object> options);

	public Future<Boolean> connect(InetSocketAddress address, int weight)
			throws IOException;

	public void updateSessions();
}
