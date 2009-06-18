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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.command.text.TextGetCommand;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;
import net.rubyeye.xmemcached.monitor.StatisticsHandler;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.HandlerAdapter;

/**
 * Memcached Session Handler,used for dispatching commands and session's
 * lifecycle management
 * 
 * @author dennis
 * 
 */
public class MemcachedHandler extends HandlerAdapter {

	private final StatisticsHandler statisticsHandler;

	/**
	 * On receive message from memcached server
	 */
	@Override
	public final void onReceive(final Session session, final Object msg) {
		Command command = (Command) msg;
		if (command.getMergeCount() > 0) {
			int size = ((TextGetCommand) command).getReturnValues().size();
			statisticsHandler.statistics(CommandType.GET_HIT, size);
			statisticsHandler.statistics(CommandType.GET_MISS, command
					.getMergeCount()
					- size);
		} else if (command instanceof TextGetOneCommand) {
			if (command.getResult() != null) {
				statisticsHandler.statistics(CommandType.GET_HIT);
			} else
				statisticsHandler.statistics(CommandType.GET_MISS);
		} else
			statisticsHandler.statistics(command.getCommandType());
	}

	private final MemcachedClient client;
	private static final Log log = LogFactory.getLog(MemcachedHandler.class);

	/**
	 * put command which have been sent to queue
	 */
	@Override
	public final void onMessageSent(Session session, Object msg) {
		Command command = (Command) msg;
		command.setStatus(OperationStatus.SENT);
		// It is no noreply
		if (!command.isNoreply())
			((MemcachedTCPSession) session).addCommand(command);
	}

	@Override
	public void onException(Session session, Throwable t) {
		super.onException(session, t);
		t.printStackTrace();
	}

	/**
	 * On session started
	 */
	@Override
	public void onSessionStarted(Session session) {
		// 启用阻塞读写，因为memcached通常跑在局域网内，网络状况良好，采用阻塞读写效率更好
		// session.setUseBlockingWrite(true);

		// use blocking read in LAN
		session.setUseBlockingRead(true);
	}

	/**
	 * Check if have to reconnect on session closed
	 */
	@Override
	public final void onSessionClosed(Session session) {
		this.client.getConnector().removeSession((MemcachedTCPSession) session);
		if (((MemcachedTCPSession) session).isAllowReconnect())
			reconnect(session);
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
			this.client.getConnector().addToWatingQueue(
					new ReconnectRequest(session.getRemoteSocketAddress(), 0,
							((MemcachedTCPSession) session).getWeight()));
		}
	}

	public MemcachedHandler(MemcachedClient client) {
		super();
		this.client = client;
		this.statisticsHandler = new StatisticsHandler();
	}

}
