/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientStateListener;
import net.rubyeye.xmemcached.auth.AuthMemcachedConnectListener;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.MapReturnValueAware;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.command.binary.BinaryGetCommand;
import net.rubyeye.xmemcached.command.binary.BinaryVersionCommand;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;
import net.rubyeye.xmemcached.command.text.TextVersionCommand;
import net.rubyeye.xmemcached.monitor.StatisticsHandler;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.networking.MemcachedSessionConnectListener;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;
import net.rubyeye.xmemcached.utils.Protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;

/**
 * Memcached Session Handler,used for dispatching commands and session's
 * lifecycle management
 * 
 * @author dennis
 * 
 */
public class MemcachedHandler extends HandlerAdapter {

	private final StatisticsHandler statisticsHandler;

	private ExecutorService heartBeatThreadPool;

	private final MemcachedSessionConnectListener listener;

	/**
	 * On receive message from memcached server
	 */
	@Override
	public final void onMessageReceived(final Session session, final Object msg) {
		Command command = (Command) msg;
		if (this.statisticsHandler.isStatistics()) {
			if (command.getMergeCount() > 0) {
				int size = ((MapReturnValueAware) command).getReturnValues()
						.size();
				this.statisticsHandler.statistics(CommandType.GET_HIT, size);
				this.statisticsHandler.statistics(CommandType.GET_MISS, command
						.getMergeCount()
						- size);
			} else if (command instanceof TextGetOneCommand
					|| command instanceof BinaryGetCommand) {
				if (command.getResult() != null) {
					this.statisticsHandler.statistics(CommandType.GET_HIT);
				} else {
					this.statisticsHandler.statistics(CommandType.GET_MISS);
				}
			} else {
				this.statisticsHandler.statistics(command.getCommandType());
			}
		}
	}

	private volatile boolean enableHeartBeat = true;

	public void setEnableHeartBeat(boolean enableHeartBeat) {
		this.enableHeartBeat = enableHeartBeat;
	}

	private final MemcachedClient client;
	private static final Logger log = LoggerFactory
			.getLogger(MemcachedHandler.class);

	/**
	 * put command which have been sent to queue
	 */
	@Override
	public final void onMessageSent(Session session, Object msg) {
		Command command = (Command) msg;
		command.setStatus(OperationStatus.SENT);
		if (!command.isNoreply()
				|| this.client.getProtocol() == Protocol.Binary) {
			((MemcachedTCPSession) session).addCommand(command);
		}
	}

	@Override
	public void onExceptionCaught(Session session, Throwable throwable) {
		log.error("XMemcached network layout exception", throwable);
	}

	/**
	 * On session started
	 */
	@Override
	public void onSessionStarted(Session session) {
		session.setUseBlockingRead(true);
		session.setAttribute(HEART_BEAT_FAIL_COUNT_ATTR, new AtomicInteger(0));
		for (MemcachedClientStateListener listener : this.client
				.getStateListeners()) {
			listener.onConnected(this.client, session.getRemoteSocketAddress());
		}
		listener.onConnect((MemcachedTCPSession) session, client);
	}

	/**
	 * Check if have to reconnect on session closed
	 */
	@Override
	public final void onSessionClosed(Session session) {
		this.client.getConnector().removeSession(session);
		if (this.client.getConnector().isStarted()
				&& ((MemcachedSession) session).isAllowReconnect()) {
			reconnect(session);
		}
		for (MemcachedClientStateListener listener : this.client
				.getStateListeners()) {
			listener.onDisconnected(this.client, session
					.getRemoteSocketAddress());
		}
	}

	/**
	 * Do a heartbeat action
	 */
	@Override
	public void onSessionIdle(Session session) {
		if (this.enableHeartBeat) {
			log.debug("Session (%s) is idle,send heartbeat", session
					.getRemoteSocketAddress() == null ? "unknown" : session
					.getRemoteSocketAddress().toString());
			Command versionCommand = null;
			CountDownLatch latch = new CountDownLatch(1);
			if (this.client.getProtocol() == Protocol.Binary) {
				versionCommand = new BinaryVersionCommand(latch, session
						.getRemoteSocketAddress());

			} else {
				versionCommand = new TextVersionCommand(latch, session
						.getRemoteSocketAddress());
			}
			session.write(versionCommand);
			// Start a check thread,avoid blocking reactor thread
			if (this.heartBeatThreadPool != null) {
				this.heartBeatThreadPool.execute(new CheckHeartResultThread(
						versionCommand, session));
			}
		}

	}

	private static final String HEART_BEAT_FAIL_COUNT_ATTR = "heartBeatFailCount";
	private static final int MAX_HEART_BEAT_FAIL_COUNT = 5;

	final static class CheckHeartResultThread implements Runnable {

		private final Command versionCommand;
		private final Session session;

		public CheckHeartResultThread(Command versionCommand, Session session) {
			super();
			this.versionCommand = versionCommand;
			this.session = session;
		}

		public void run() {
			try {
				AtomicInteger heartBeatFailCount = (AtomicInteger) this.session
						.getAttribute(HEART_BEAT_FAIL_COUNT_ATTR);
				if (heartBeatFailCount != null) {
					if (!this.versionCommand.getLatch().await(2000,
							TimeUnit.MILLISECONDS)) {
						heartBeatFailCount.incrementAndGet();
					}
					if (this.versionCommand.getResult() == null) {
						heartBeatFailCount.incrementAndGet();
					} else {
						// reset
						heartBeatFailCount.set(0);
					}
					// 10 times fail
					if (heartBeatFailCount.get() > MAX_HEART_BEAT_FAIL_COUNT) {
						log
								.warn("Session("
										+ this.session.getRemoteSocketAddress()
										+ ") heartbeat fail 10 times,close session and try to heal it");
						this.session.close();// close session
						heartBeatFailCount.set(0);
					}
				}
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * Auto reconect to memcached server
	 * 
	 * @param session
	 */
	protected void reconnect(Session session) {
		if (!this.client.isShutdown()) {
			log.debug("Add reconnectRequest to connector "
					+ session.getRemoteSocketAddress());
			MemcachedSession memcachedTCPSession = (MemcachedSession) session;
			InetSocketAddressWrapper inetSocketAddressWrapper = new InetSocketAddressWrapper(
					session.getRemoteSocketAddress(), memcachedTCPSession
							.getOrder());
			this.client.getConnector().addToWatingQueue(
					new ReconnectRequest(inetSocketAddressWrapper, 0,
							((MemcachedSession) session).getWeight()));
		}
	}

	public void stop() {
		this.heartBeatThreadPool.shutdown();
	}

	public void start() {
		int serverSize = this.client.getAvaliableServers().size();
		this.heartBeatThreadPool = Executors
				.newFixedThreadPool(serverSize == 0 ? Runtime.getRuntime()
						.availableProcessors() : serverSize);
	}

	public MemcachedHandler(MemcachedClient client) {
		super();
		this.client = client;
		listener = new AuthMemcachedConnectListener();
		this.statisticsHandler = new StatisticsHandler();

	}

}
