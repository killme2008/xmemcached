/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rubyeye.xmemcached;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.TCPConnectorController;
import com.google.code.yanf4j.util.Queue;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import net.rubyeye.xmemcached.utils.SimpleQueue;

/**
 * 针对memcached的连接管理类
 * 
 * @author dennis
 */
public class MemcachedConnector extends TCPConnectorController {

	protected MemcachedProtocolHandler memcachedProtocolHandler;
	private MemcachedTCPSession session;

	public void setMemcachedProtocolHandler(
			MemcachedProtocolHandler memcachedProtocolHandler) {
		this.memcachedProtocolHandler = memcachedProtocolHandler;
	}

	public MemcachedProtocolHandler getMemcachedProtocolHandler() {
		return this.memcachedProtocolHandler;
	}

	public MemcachedConnector() {
		super();
	}

	public MemcachedConnector(Configuration configuration) {
		super(configuration, null);

	}

	/**
	 * 使用扩展queue
	 */
	protected Queue<Session.WriteMessage> buildQueue() {
		return new SimpleQueue<Session.WriteMessage>();
	}

	public MemcachedTCPSession getSession() {
		return session;
	}

	public void setSession(MemcachedTCPSession session) {
		this.session = session;
	}

	@SuppressWarnings("unchecked")
	public MemcachedConnector(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, codecFactory);
	}

	protected Session buildSession(SocketChannel sc, SelectionKey selectionKey) {
		Queue<Session.WriteMessage> queue = buildQueue();
		session = new MemcachedTCPSession(sc, selectionKey, handler,
				getReactor(), getCodecFactory(), configuration
						.getSessionReadBufferSize(), statistics, queue,
				sessionTimeout, handleReadWriteConcurrently);
		session.setMemcachedProtocolHandler(this.getMemcachedProtocolHandler());
		return session;
	}
}
