package com.google.code.yanf4j.nio;

/**
 *Copyright [2008] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.impl.FutureImpl;
import com.google.code.yanf4j.nio.impl.NioTCPSession;
import com.google.code.yanf4j.nio.impl.SocketChannelController;

/**
 * Controller for client connecting
 * 
 * @author dennis
 * 
 */
public class TCPConnectorController extends SocketChannelController {
	protected SocketChannel socketChannel;

	protected SocketAddress remoteAddress;

	protected NioTCPSession session;

	private FutureImpl<Boolean> connectFuture;

	public TCPConnectorController() {
		super();
	}

	public TCPConnectorController(Configuration configuration) {
		super(configuration, null, null);

	}

	public SocketAddress getRemoteSocketAddress() {
		return this.remoteAddress;
	}

	public TCPConnectorController(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
	}

	public Future<Boolean> connect(SocketAddress remoteAddr) throws IOException {
		if (this.started) {
			throw new IllegalStateException("SocketChannel has been connected");
		}
		if (remoteAddr == null) {
			throw new IllegalArgumentException("Null remote address");
		}
		this.connectFuture = new FutureImpl<Boolean>();
		this.remoteAddress = remoteAddr;
		this.start();
		return this.connectFuture;
	}

	public void awaitConnectUnInterrupt() throws IOException {
		if (this.connectFuture == null) {
			throw new IllegalStateException(
					"The connector has not been started");
		}
		try {
			this.connectFuture.get();
		} catch (ExecutionException e) {
			throw new IOException("await connect error:" + e.getMessage());
		} catch (InterruptedException e) {

		}
	}

	public Future<Boolean> send(Object msg) {
		if (this.session == null) {
			throw new IllegalStateException(
					"SocketChannel has not been connected");
		}
		return this.session.asyncWrite(msg);
	}

	public boolean isConnected() {
		return this.session != null && !this.session.isClosed();
	}

	public void disconnect() throws IOException {
		stop();
	}

	/**
	 * �Ͽ����Ӳ�����
	 * 
	 * @throws IOException
	 */
	public Future<Boolean> reconnect() throws IOException {
		if (!this.started) {
			FutureImpl<Boolean> future = new FutureImpl<Boolean>();
			future.setResult(false);
			return future;
		}
		this.session.close();
		this.connectFuture = new FutureImpl<Boolean>();
		doStart();
		return this.connectFuture;
	}

	/**
	 * �Ͽ���ǰ���ӣ��������µ�ַremoteAddr
	 * 
	 * @param remoteAddr
	 * @throws IOException
	 */
	public void reconnect(SocketAddress remoteAddr) throws IOException {
		if (!this.started) {
			return;
		}
		this.remoteAddress = remoteAddr;
		this.session.close();
		doStart();
	}

	@Override
	protected void doStart() throws IOException {
		initialSelectorManager();
		this.socketChannel = SocketChannel.open();
		configureSocketChannel(this.socketChannel);
		if (this.localSocketAddress != null) {
			this.socketChannel.socket().bind(this.localSocketAddress);
		}
		if (!this.socketChannel.connect(this.remoteAddress)) {
			this.selectorManager.registerChannel(this.socketChannel,
					SelectionKey.OP_CONNECT, null);
		} else {
			createSession(this.socketChannel);
		}
	}

	@Override
	public void onConnect(SelectionKey key) throws IOException {
		key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);

		try {
			if (!((SocketChannel) key.channel()).finishConnect()) {
				throw new IOException("Connect Fail");
			}
			createSession((SocketChannel) key.channel());
			this.connectFuture.setResult(true);
		} catch (Exception e) {
			log.error("Connect error", e);
			this.connectFuture.failure(e);
		}
	}

	protected void createSession(SocketChannel socketChannel) {
		this.session = (NioTCPSession) buildSession(socketChannel);
		this.selectorManager.registerSession(this.session,
				EventType.ENABLE_READ);
		setLocalSocketAddress((InetSocketAddress) socketChannel.socket()
				.getLocalSocketAddress());
		this.session.start();
		this.session.onEvent(EventType.CONNECTED, null);

	}

	public void closeChannel(Selector selector) throws IOException {
		if (this.session != null) {
			this.session.close();
			this.session = null;
		}
		selector.selectNow();
	}
}
