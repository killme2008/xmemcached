/**
 *Copyright [2009-2010] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.AbstractController;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.nio.SelectionKeyHandler;
import com.google.code.yanf4j.util.SystemUtils;

/**
 * Base nio controller
 * 
 * @author dennis
 * 
 */
public abstract class NioController extends AbstractController implements
		SelectionKeyHandler {

	protected SelectorManager selectorManager;

	/**
	 * Reactor count
	 */
	protected int selectorPoolSize = SystemUtils.getSystemThreadCount();

	/**
	 * @see setSelectorPoolSize
	 * @return
	 */
	public int getSelectorPoolSize() {
		return this.selectorPoolSize;
	}

	public void setSelectorPoolSize(int selectorPoolSize) {
		if (isStarted()) {
			throw new IllegalStateException("Controller has been started");
		}
		this.selectorPoolSize = selectorPoolSize;
	}

	public NioController() {
		super();
	}

	public NioController(Configuration configuration, CodecFactory codecFactory) {
		super(configuration, codecFactory);
	}

	public NioController(Configuration configuration, Handler handler,
			CodecFactory codecFactory) {
		super(configuration, handler, codecFactory);
	}

	public NioController(Configuration configuration) {
		super(configuration);
	}

	/**
	 * Write task
	 * 
	 * @author dennis
	 * 
	 */
	private final class WriteTask implements Runnable {
		private final SelectionKey key;

		private WriteTask(SelectionKey key) {
			this.key = key;
		}

		public final void run() {
			dispatchWriteEvent(this.key);
		}
	}

	/**
	 * Read task
	 * 
	 * @author dennis
	 * 
	 */
	private final class ReadTask implements Runnable {
		private final SelectionKey key;

		private ReadTask(SelectionKey key) {
			this.key = key;
		}

		public final void run() {
			dispatchReadEvent(this.key);
		}
	}

	public final SelectorManager getSelectorManager() {
		return this.selectorManager;
	}

	@Override
	protected void start0() throws IOException {
		try {
			initialSelectorManager();
			doStart();
		} catch (IOException e) {
			log.error("Start server error", e);
			notifyException(e);
			stop();
			throw e;
		}

	}

	/**
	 * Start selector manager
	 * 
	 * @throws IOException
	 */
	protected void initialSelectorManager() throws IOException {
		if (this.selectorManager == null) {
			this.selectorManager = new SelectorManager(this.selectorPoolSize, this,
					this.configuration);
			this.selectorManager.start();
		}
	}

	/**
	 * Inner startup
	 * 
	 * @throws IOException
	 */
	protected abstract void doStart() throws IOException;

	/**
	 * Read event occured
	 */
	public void onRead(SelectionKey key) {
		if (this.readEventDispatcher == null) {
			dispatchReadEvent(key);
		} else {
			this.readEventDispatcher.dispatch(new ReadTask(key));
		}
	}

	/**
	 * Writable event occured
	 */
	public void onWrite(final SelectionKey key) {
		if (this.writeEventDispatcher == null) {
			dispatchWriteEvent(key);
		} else {
			this.writeEventDispatcher.dispatch(new WriteTask(key));
		}
	}

	/**
	 * Cancel selection key
	 */
	public void closeSelectionKey(SelectionKey key) {
		if (key.attachment() instanceof Session) {
			Session session = (Session) key.attachment();
			if (session != null) {
				session.close();
			}
		}
	}

	/**
	 * Dispatch read event
	 * 
	 * @param key
	 * @return
	 */
	protected abstract void dispatchReadEvent(final SelectionKey key);

	/**
	 * Dispatch write event
	 * 
	 * @param key
	 * @return
	 */
	protected abstract void dispatchWriteEvent(final SelectionKey key);

	@Override
	protected void stop0() throws IOException {
		if (this.selectorManager == null || !this.selectorManager.isStarted()) {
			return;
		}
		this.selectorManager.stop();
	}

	public synchronized void bind(int port) throws IOException {
		if (isStarted()) {
			throw new IllegalStateException("Server has been bind to "
					+ getLocalSocketAddress());
		}
		bind(new InetSocketAddress(port));
	}

	/**
	 * Build nio session config
	 * 
	 * @param sc
	 * @param queue
	 * @return
	 */
	protected final NioSessionConfig buildSessionConfig(SelectableChannel sc,
			Queue<WriteMessage> queue) {
		final NioSessionConfig sessionConfig = new NioSessionConfig(sc,
				getHandler(), this.selectorManager, getCodecFactory(),
				getStatistics(), queue, this.dispatchMessageDispatcher,
				isHandleReadWriteConcurrently(), this.sessionTimeout, this.configuration
						.getSessionIdleTimeout());
		return sessionConfig;
	}

}
