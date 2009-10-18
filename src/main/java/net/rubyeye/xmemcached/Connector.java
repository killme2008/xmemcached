package net.rubyeye.xmemcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Future;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;

import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.ControllerStateListener;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;

public interface Connector {
	public void setOptimizeMergeBuffer(boolean optimiezeMergeBuffer);

	public void setMergeFactor(int factor);

	public void setOptimizeGet(boolean optimizeGet);
	
	public void removeSession(Session session);

	public Queue<Session> getSessionByAddress(InetSocketAddress address);

	public Set<Session> getSessionSet();

	public void setHealSessionInterval(long interval);

	public void send(Command packet)throws MemcachedException ;

	public void removeStateListener(ControllerStateListener listener);

	public void addStateListener(ControllerStateListener listener);

	public void setConnectionPoolSize(int connectionPoolSize);

	public void setBufferAllocator(BufferAllocator bufferAllocator);

	public List<Session> getSessionListBySocketAddress(InetSocketAddress address);

	public void start()throws IOException;
	
	public void removeReconnectRequest(InetSocketAddress address);
	
	public void addToWatingQueue(ReconnectRequest request);
	
	public Future<Boolean> connect(InetSocketAddress address,int weight)throws IOException;

	public void updateSessions();

	public void setHandler(Handler handler);

	public void setCodecFactory(CodecFactory codecFactory);

	public void setSessionTimeout(long timeout);

	public void setSocketOptions(Map<SocketOption, Object> options);

	public void stop()throws IOException;

}
