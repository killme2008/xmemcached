/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */

package com.google.code.yanf4j.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.StandardSocketOption;
import com.google.code.yanf4j.nio.impl.SocketChannelController;

/**
 * Controller for tcp server
 * 
 * @author dennis
 */
public class TCPController extends SocketChannelController 
		 {

	private ServerSocketChannel serverSocketChannel;

	/**
	 * Accept backlog queue size
	 */
	private int backlog = 500; // default 500

	public int getBacklog() {
		return this.backlog;
	}

	public void setBacklog(int backlog) {
		if (isStarted()) {
			throw new IllegalStateException();
		}
		if (backlog < 0) {
			throw new IllegalArgumentException("backlog<0");
		}
		this.backlog = backlog;
	}

	public TCPController() {
		super();
	}

	public TCPController(Configuration configuration) {
		super(configuration, null, null);

	}

	public TCPController(Configuration configuration, CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
	}

	public TCPController(Configuration configuration, Handler handler,
			CodecFactory codecFactory) {
		super(configuration, handler, codecFactory);
	}

	private int connectionTime, latency, bandwidth;

	public void setPerformancePreferences(int connectionTime, int latency,
			int bandwidth) {
		this.connectionTime = connectionTime;
		this.latency = latency;
		this.bandwidth = bandwidth;
	}

	@Override
	protected void doStart() throws IOException {
		this.serverSocketChannel = ServerSocketChannel.open();
		this.serverSocketChannel.socket().setSoTimeout(this.soTimeout);
		if (this.connectionTime != 0 || this.latency != 0
				|| this.bandwidth != 0) {
			this.serverSocketChannel.socket().setPerformancePreferences(
					this.connectionTime, this.latency, this.bandwidth);
		}
		this.serverSocketChannel.configureBlocking(false);

		if (this.socketOptions.get(StandardSocketOption.SO_REUSEADDR) != null) {
			this.serverSocketChannel.socket().setReuseAddress(
					StandardSocketOption.SO_REUSEADDR.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_REUSEADDR)));
		}
		if (this.socketOptions.get(StandardSocketOption.SO_RCVBUF) != null) {
			this.serverSocketChannel.socket().setReceiveBufferSize(
					StandardSocketOption.SO_RCVBUF.type().cast(
							this.socketOptions
									.get(StandardSocketOption.SO_RCVBUF)));

		}
		if (this.localSocketAddress != null) {
			this.serverSocketChannel.socket().bind(this.localSocketAddress,
					this.backlog);
		} else {
			this.serverSocketChannel.socket().bind(
					new InetSocketAddress("localhost", 0), this.backlog);
		}
		setLocalSocketAddress((InetSocketAddress) this.serverSocketChannel
				.socket().getLocalSocketAddress());
		this.selectorManager.registerChannel(this.serverSocketChannel,
				SelectionKey.OP_ACCEPT, null);
	}

	@Override
	public void onAccept(SelectionKey selectionKey) throws IOException {
		// �������رգ�ȡ������
		if (!this.serverSocketChannel.isOpen()) {
			selectionKey.cancel();
			return;
		}
		SocketChannel sc = null;
		try {
			sc = this.serverSocketChannel.accept();
			if (sc != null) {
				configureSocketChannel(sc);
				Session session = buildSession(sc);
				// enable read
				this.selectorManager.registerSession(session,
						EventType.ENABLE_READ);
				session.start();
				super.onAccept(selectionKey); // for statistics
			} else {
				log.debug("Accept fail");
			}
		} catch (IOException e) {
			closeAcceptChannel(selectionKey, sc);
			log.error("Accept connection error", e);
			notifyException(e);
		}
	}

	/**
	 * 
	 * @param sk
	 * @param sc
	 * @throws IOException
	 * @throws SocketException
	 */
	private void closeAcceptChannel(SelectionKey sk, SocketChannel sc)
			throws IOException, SocketException {
		if (sk != null) {
			sk.cancel();
		}
		if (sc != null) {
			sc.socket().setSoLinger(true, 0); // await TIME_WAIT status
			sc.socket().shutdownOutput();
			sc.close();
		}
	}

	public void closeChannel(Selector selector) throws IOException {
		if (this.serverSocketChannel != null) {
			this.serverSocketChannel.close();
		}
	}

	public void unbind() throws IOException {
		stop();
	}

}