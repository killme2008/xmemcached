package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.StandardSocketOption;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.nio.NioSessionConfig;

/**
 * Nio tcp socket controller
 * 
 * @author dennis
 * 
 */
public abstract class SocketChannelController extends NioController {

	protected boolean soLingerOn = false;

	public void setSoLinger(boolean on, int value) {
		this.soLingerOn = on;
		this.socketOptions.put(StandardSocketOption.SO_LINGER, value);
	}

	public SocketChannelController() {
		super();
	}

	public SocketChannelController(Configuration configuration) {
		super(configuration, null, null);

	}

	public SocketChannelController(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
	}

	public SocketChannelController(Configuration configuration,
			Handler handler, CodecFactory codecFactory) {
		super(configuration, handler, codecFactory);
	}

	@Override
	protected final void dispatchReadEvent(SelectionKey key) {
		Session session = (Session) key.attachment();
		if (session != null) {
			((NioSession) session).onEvent(EventType.READABLE, key.selector());
		} else {
			log
					.warn("Could not find session for readable event,maybe it is closed");
		}
	}

	@Override
	protected final void dispatchWriteEvent(SelectionKey key) {
		Session session = (Session) key.attachment();
		if (session != null) {
			((NioSession) session).onEvent(EventType.WRITEABLE, key.selector());
		} else {
			log
					.warn("Could not find session for writable event,maybe it is closed");
		}

	}

	protected NioSession buildSession(SocketChannel sc) {
		Queue<WriteMessage> queue = buildQueue();
		NioSessionConfig sessionConfig = buildSessionConfig(sc, queue);
		NioSession session = new NioTCPSession(sessionConfig,
				this.configuration.getSessionReadBufferSize());
		return session;
	}

	/**
	 * Confiure socket channel
	 * 
	 * @param sc
	 * @throws IOException
	 */
	protected final void configureSocketChannel(SocketChannel sc)
			throws IOException {
		sc.socket().setSoTimeout(this.soTimeout);
		sc.configureBlocking(false);
		if (this.socketOptions.get(StandardSocketOption.SO_REUSEADDR) != null) {
			sc.socket().setReuseAddress(
					StandardSocketOption.SO_REUSEADDR.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_REUSEADDR)));
		}
		if (this.socketOptions.get(StandardSocketOption.SO_SNDBUF) != null) {
			sc.socket().setSendBufferSize(
					StandardSocketOption.SO_SNDBUF.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_SNDBUF)));
		}
		if (this.socketOptions.get(StandardSocketOption.SO_KEEPALIVE) != null) {
			sc.socket().setKeepAlive(
					StandardSocketOption.SO_KEEPALIVE.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_KEEPALIVE)));
		}
		if (this.socketOptions.get(StandardSocketOption.SO_LINGER) != null) {
			sc.socket().setSoLinger(
					this.soLingerOn,
					StandardSocketOption.SO_LINGER.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_LINGER)));
		}
		if (this.socketOptions.get(StandardSocketOption.SO_RCVBUF) != null) {
			sc.socket().setReceiveBufferSize(
					StandardSocketOption.SO_RCVBUF.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_RCVBUF)));

		}
		if (this.socketOptions.get(StandardSocketOption.TCP_NODELAY) != null) {
			sc.socket().setTcpNoDelay(
					StandardSocketOption.TCP_NODELAY.type().cast(
							this.socketOptions
									.get(StandardSocketOption.TCP_NODELAY)));
		}
	}

}
