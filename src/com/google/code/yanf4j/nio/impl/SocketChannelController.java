package com.google.code.yanf4j.nio.impl;

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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.util.Queue;

/**
 * tcp controller基类
 * 
 * @author dennis
 * 
 */
public abstract class SocketChannelController extends AbstractController {
	protected boolean tcpNoDelay = false;

	public SocketChannelController() {
		super();
	}

	public boolean isTcpNoDelay() {
		return this.tcpNoDelay;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public SocketChannelController(Configuration configuration) {
		super(configuration, null, null);

	}

	@SuppressWarnings("unchecked")
	public SocketChannelController(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
	}

	@SuppressWarnings("unchecked")
	public SocketChannelController(Configuration configuration,
			Handler handler, CodecFactory codecFactory) {
		super(configuration, handler, codecFactory);
		setTcpNoDelay(configuration.isTcpNoDelay());
	}

	protected Runnable getReadHandler(final SelectionKey key) {
		final Session session = (Session) key.attachment();
		return new Runnable() {
			public void run() {
				session.onEvent(EventType.READABLE, selector);
			}
		};
	}

	protected Session buildSession(SocketChannel sc, SelectionKey selectionKey) {
		Queue<Session.WriteMessage> queue = buildQueue();
		Session session = new DefaultTCPSession(sc, selectionKey, handler,
				getReactor(), getCodecFactory(), configuration
						.getSessionReadBufferSize(), statistics, queue,
				sessionTimeout, handleReadWriteConcurrently);
		return session;
	}

	public void onWrite(SelectionKey key) {
		if (key.attachment() != null)
			((Session) (key.attachment())).onEvent(EventType.WRITEABLE,
					selector);
	}
}
