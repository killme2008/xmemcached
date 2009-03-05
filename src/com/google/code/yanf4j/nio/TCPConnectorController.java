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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.net.SocketAddress;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.impl.SocketChannelController;
import com.google.code.yanf4j.nio.util.EventType;

/**
 * 用于客户端连接的Controller主控类
 * 
 * @author dennis
 * 
 */
public class TCPConnectorController extends SocketChannelController {
	private SocketChannel socketChannel;

	private SocketAddress remoteAddress;

	private volatile boolean connected;

	private int sendBufferSize;

	public TCPConnectorController() {
		super();
	}

	public TCPConnectorController(Configuration configuration) {
		super(configuration, null, null);

	}

	public int getSendBufferSize() {
		return sendBufferSize;
	}

	public void setSendBufferSize(int sendBufferSize) {
		if (this.connected)
			throw new IllegalStateException("SocketChannel has been connected");
		if (sendBufferSize <= 0)
			throw new IllegalArgumentException("sendBufferSize<=0");
		this.sendBufferSize = sendBufferSize;
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	@SuppressWarnings("unchecked")
	public TCPConnectorController(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
	}

	public void connect(SocketAddress remoteAddr) throws IOException {
		if (this.connected)
			throw new IllegalStateException("SocketChannel has been connected");
		this.remoteAddress = remoteAddr;
		this.start();
	}

	public synchronized void awaitForConnected() throws InterruptedException {
		if (this.connected)
			return;
		this.wait();
	}

	public void send(Object msg) throws InterruptedException {
		if (!this.connected || this.selectionKey.attachment() == null)
			throw new IllegalStateException(
					"SocketChannel has not been connected");
		((Session) this.selectionKey.attachment()).send(msg);
	}

	public boolean isConnected() {
		return connected;
	}

	public void reconnect() throws IOException {
		if (!this.connected)
			return;
		if (this.selectionKey.attachment() != null) {
			((Session) (this.selectionKey.attachment())).close();
		}
		this.connected = false;
		doStart();
	}

	public void reconnect(SocketAddress remoteAddr) throws IOException {
		if (!this.connected)
			return;
		this.remoteAddress = remoteAddr;
		if (this.selectionKey.attachment() != null) {
			((Session) (this.selectionKey.attachment())).close();
		}
		this.connected = false;
		doStart();
	}

	@Override
	protected void doStart() throws IOException {
		this.socketChannel = SocketChannel.open();
		this.socketChannel.configureBlocking(false);
		socketChannel.socket().setSoTimeout(timeout);
		socketChannel.socket().setReuseAddress(reuseAddress); // 重用端口
		if (this.receiveBufferSize > 0)
			socketChannel.socket().setReceiveBufferSize(receiveBufferSize); // 设置接收缓冲区
		socketChannel.socket().bind(this.socketAddress);
		if (this.sendBufferSize > 0)
			socketChannel.socket().setSendBufferSize(this.sendBufferSize);

		if (!this.socketChannel.connect(this.remoteAddress))
			this.selectionKey = socketChannel.register(selector,
					SelectionKey.OP_CONNECT);
		else {
			createSession(this.selectionKey);
			setConnected(true);
			synchronized (this) {
				this.notifyAll();
			}
		}

	}

	void setConnected(boolean connected) {
		this.connected = connected;
	}

	public void onConnect(SelectionKey key) throws IOException {
		key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		try {
			if (!this.socketChannel.finishConnect())
				throw new IOException("Connect Fail");
			createSession(key);
			setConnected(true);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	private void createSession(SelectionKey key) {
		Session session = buildSession(this.socketChannel, key);
		session.onEvent(EventType.ENABLE_READ, selector);
		key.attach(session);
		session.start();
		session.onEvent(EventType.CONNECTED, selector);
		selector.wakeup();
	}

	public void closeChannel() throws IOException {
		if (socketChannel != null)
			socketChannel.close();
	}
}
