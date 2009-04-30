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
package net.rubyeye.xmemcached;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.OperationStatus;
import net.rubyeye.xmemcached.command.Command.CommandType;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.HandlerAdapter;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.monitor.StatisticsHandler;

import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;

/**
 * 核心类，负责协议解析和消息派发
 * 
 * @author dennis
 * 
 */
public class MemcachedHandler extends HandlerAdapter<Command> implements
		MemcachedProtocolHandler {

	private static final ByteBuffer SPLIT = ByteBuffer.wrap(Command.SPLIT
			.getBytes());
	/**
	 * shift-and算法匹配器，用于匹配行
	 */
	private static final ByteBufferMatcher SPLIT_MATCHER = new ShiftAndByteBufferMatcher(
			SPLIT);

	private StatisticsHandler statisticsHandler;

	/**
	 * 返回boolean值并唤醒
	 * 
	 * @param result
	 * @return
	 */
	private final boolean notifyBoolean(MemcachedTCPSession session,
			Boolean result, Command.CommandType expectedCmdType,
			Command.CommandType... otherExpectedCmdType) {
		final Command executingCmd = session.getCurrentExecutingCommand();
		boolean isExpected = (executingCmd.getCommandType() == expectedCmdType);
		if (!isExpected) {
			for (Command.CommandType cmdType : otherExpectedCmdType) {
				if (cmdType == executingCmd.getCommandType()) {
					isExpected = true;
					break;
				}
			}
		}
		statistics(executingCmd.getCommandType());
		if (!isExpected) {
			session.close();
			return false;
		}
		executingCmd.setResult(result);
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
		session.resetStatus();
		return true;
	}

	/**
	 * 解析状态
	 * 
	 * @author dennis
	 * 
	 */
	enum ParseStatus {

		NULL, GET, END, STORED, NOT_STORED, ERROR, CLIENT_ERROR, SERVER_ERROR, DELETED, NOT_FOUND, VERSION, INCR, EXISTS;
	}

	int count = 0;

	public boolean onReceive(final MemcachedTCPSession session,
			final ByteBuffer buffer) {
		int origPos = buffer.position();
		int origLimit = buffer.limit();
		LABEL: while (true) {
			switch (session.status) {
			case NULL:
				nextLine(session, buffer);
				if (session.currentLine == null) {
					return false;
				}
				if (session.currentLine.startsWith("VALUE")) {
					session.currentValues = new HashMap<String, CachedData>();
					session.status = ParseStatus.GET;
				} else if (session.currentLine.equals("STORED")) {
					return notifyBoolean(session, Boolean.TRUE,
							Command.CommandType.SET, Command.CommandType.CAS,
							Command.CommandType.ADD,
							Command.CommandType.REPLACE,
							Command.CommandType.APPEND,
							Command.CommandType.PREPEND);
				} else if (session.currentLine.equals("DELETED")) {
					return notifyBoolean(session, Boolean.TRUE,
							Command.CommandType.DELETE);
				} else if (session.currentLine.equals("END")) {
					return parseEndCommand(session);
				} else if (session.currentLine.equals("EXISTS")) {
					return notifyBoolean(session, Boolean.FALSE,
							Command.CommandType.CAS);
				} else if (session.currentLine.equals("NOT_STORED")) {
					return notifyBoolean(session, Boolean.FALSE,
							Command.CommandType.SET, Command.CommandType.ADD,
							Command.CommandType.REPLACE,
							Command.CommandType.APPEND,
							Command.CommandType.PREPEND);
				} else if (session.currentLine.equals("NOT_FOUND")) {
					return notifyBoolean(session, Boolean.FALSE,
							Command.CommandType.DELETE,
							Command.CommandType.CAS, Command.CommandType.INCR,
							Command.CommandType.DECR);
				} else if (session.currentLine.equals("ERROR")) {
					return parseException(session);
				} else if (session.currentLine.startsWith("CLIENT_ERROR")) {
					return parseClientException(session);
				} else if (session.currentLine.startsWith("SERVER_ERROR")) {
					return parseServerException(session);
				} else if (session.currentLine.startsWith("VERSION ")) {
					return parseVersionCommand(session);
				} else {
					return parseIncrDecrCommand(session);
				}
				if (!session.status.equals(ParseStatus.NULL)) {
					continue LABEL;
				} else {
					log.error("unknow response:" + session.currentLine);
					throw new IllegalStateException("unknown response:"
							+ session.currentLine);
				}
			case GET:
				return parseGet(session, buffer, origPos, origLimit);
			default:
				return false;

			}
		}
	}

	/**
	 * 解析get协议response
	 * 
	 * @param buffer
	 * @param origPos
	 * @param origLimit
	 * @return
	 */
	private final boolean parseGet(MemcachedTCPSession session,
			ByteBuffer buffer, int origPos, int origLimit) {
		while (true) {
			nextLine(session, buffer);
			if (session.currentLine == null) {
				return false;
			}
			if (session.currentLine.equals("END")) {
				Command executingCommand = session.getCurrentExecutingCommand();
				if (executingCommand == null) {
					return false;
				}
				if (executingCommand.getCommandType() == Command.CommandType.GET_MANY
						|| executingCommand.getCommandType() == Command.CommandType.GETS_MANY) {
					processGetManyCommand(session, session.currentValues,
							executingCommand);
				} else if (executingCommand.getCommandType() == Command.CommandType.GET_ONE
						|| executingCommand.getCommandType() == Command.CommandType.GETS_ONE) {
					processGetOneCommand(session, session.currentValues,
							executingCommand);
				} else {
					session.close();
					return false;
				}
				session.currentValues = null;
				session.resetStatus();
				return true;
			} else if (session.currentLine.startsWith("VALUE")) {
				String[] items = session.currentLine.split(" ");
				int flag = Integer.parseInt(items[2]);
				int dataLen = Integer.parseInt(items[3]);
				// 不够数据，返回
				if (buffer.remaining() < dataLen + 2) {
					buffer.position(origPos).limit(origLimit);
					session.currentLine = null;
					return false;
				}
				// 可能是gets操作
				long casId = -1;
				if (items.length >= 5) {
					casId = Long.parseLong(items[4]);
				}
				byte[] data = new byte[dataLen];
				buffer.get(data);
				session.currentValues.put(items[1], new CachedData(flag, data,
						dataLen, casId));
				buffer.position(buffer.position() + SPLIT.remaining());
				session.currentLine = null;
			} else {
				buffer.position(origPos).limit(origLimit);
				session.currentLine = null;
				return false;
			}

		}
	}

	/**
	 * 解析get协议返回空
	 * 
	 * @return
	 */
	private final boolean parseEndCommand(MemcachedTCPSession session) {
		Command executingCmd = session.getCurrentExecutingCommand();
		Command.CommandType cmdType = executingCmd.getCommandType();
		if (cmdType != Command.CommandType.GET_ONE
				&& cmdType != Command.CommandType.GETS_ONE
				&& cmdType != Command.CommandType.GET_MANY
				&& cmdType != Command.CommandType.GETS_MANY) {
			session.close();
			return false;
		}
		if (executingCmd.getCommandType() == Command.CommandType.GET_ONE)
			statistics(Command.CommandType.GET_MSS);
		else
			statistics(cmdType);
		int mergCount = executingCmd.getMergeCount();
		if (mergCount < 0) {
			// single
			executingCmd.getLatch().countDown();
			executingCmd.setStatus(OperationStatus.DONE);
		} else {
			// merge get
			List<Command> mergeCommands = executingCmd.getMergeCommands();
			for (Command nextCommand : mergeCommands) {
				nextCommand.getLatch().countDown(); // notify

				nextCommand.setStatus(OperationStatus.DONE);
			}

		}
		session.resetStatus();
		return true;

	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private static final boolean parseException(MemcachedTCPSession session) {
		Command executingCmd = session.getCurrentExecutingCommand();

		final UnknownCommandException exception = new UnknownCommandException(
				"Unknown command:" + executingCmd.toString()
						+ ",please check your memcached version");
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
		session.resetStatus();
		return true;
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private final boolean parseClientException(MemcachedTCPSession session) {
		int index = session.currentLine.indexOf(" ");
		final String error = index > 0 ? session.currentLine
				.substring(index + 1) : "unknown client error";
		Command executingCmd = session.getCurrentExecutingCommand();
		if (executingCmd.getCommandType() == Command.CommandType.GET_ONE)
			statistics(Command.CommandType.GET_MSS);
		final MemcachedClientException exception = new MemcachedClientException(
				error + ",command:" + executingCmd.toString());
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
		session.resetStatus();

		return true;
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private final boolean parseServerException(MemcachedTCPSession session) {
		int index = session.currentLine.indexOf(" ");
		final String error = index > 0 ? session.currentLine
				.substring(index + 1) : "unknown server error";
		Command executingCmd = session.getCurrentExecutingCommand();
		if (executingCmd.getCommandType() == Command.CommandType.GET_ONE)
			statistics(Command.CommandType.GET_MSS);
		final MemcachedServerException exception = new MemcachedServerException(
				error + ",command:" + executingCmd.toString());
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
		session.resetStatus();
		return true;

	}

	/**
	 * 解析version协议response
	 * 
	 * @return
	 */
	private final boolean parseVersionCommand(MemcachedTCPSession session) {
		String[] items = session.currentLine.split(" ");
		final String version = items.length > 1 ? items[1] : "unknown version";
		Command executingCmd = session.getCurrentExecutingCommand();
		if (executingCmd.getCommandType() != Command.CommandType.VERSION) {
			session.close();
			return false;
		}
		executingCmd.setResult(version);
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
		session.resetStatus();
		return true;

	}

	/**
	 * 解析incr,decr协议response
	 * 
	 * @return
	 */
	private final boolean parseIncrDecrCommand(MemcachedTCPSession session) {
		final Integer result = Integer.parseInt(session.currentLine);
		Command executingCmd = session.getCurrentExecutingCommand();
		statistics(executingCmd.getCommandType());
		if (executingCmd.getCommandType() != Command.CommandType.INCR
				&& executingCmd.getCommandType() != Command.CommandType.DECR) {
			session.close();
			return false;
		}
		executingCmd.setResult(result);
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
		session.resetStatus();

		return true;

	}

	/**
	 * 获取下一行
	 * 
	 * @param buffer
	 */
	protected static final void nextLine(MemcachedTCPSession session,
			ByteBuffer buffer) {
		if (session.currentLine != null) {
			return;
		}

		/**
		 * 测试表明采用 Shift-And算法匹配 >BM算法匹配效率 > 朴素匹配 > KMP匹配， 如果你有更好的建议，请email给我
		 */
		int index = SPLIT_MATCHER.matchFirst(buffer);
		// int index = ByteBufferUtils.indexOf(buffer, SPLIT);
		if (index >= 0) {
			int limit = buffer.limit();
			buffer.limit(index);
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			buffer.limit(limit);
			buffer.position(index + SPLIT.remaining());
			try {
				session.currentLine = new String(bytes, "utf-8");
			} catch (UnsupportedEncodingException e) {
			}

		} else {
			session.currentLine = null;
		}

	}

	/**
	 * HandlerAdapter实现，负责命令管理和派发
	 */
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	protected XMemcachedClient client;
	protected static final Log log = LogFactory.getLog(MemcachedHandler.class);

	@Override
	public void onMessageSent(Session session, Command command) {
		command.setStatus(OperationStatus.SENT);
		((MemcachedTCPSession) session).executingCmds.add(command);

	}

	@Override
	public void onSessionStarted(Session session) {
		// 启用阻塞读写，因为memcached通常跑在局域网内，网络状况良好，采用阻塞读写效率更好
		// session.setUseBlockingWrite(true);
		session.setUseBlockingRead(true);
	}

	@Override
	public void onSessionClosed(Session session) {
		this.client.getConnector().removeSession((MemcachedTCPSession) session);
		reconnect(session);
	}

	protected void reconnect(Session session) {
		if (!this.client.isShutdown()) {
			this.client.getConnector().addToWatingQueue(
					new MemcachedConnector.ReconnectRequest(session
							.getRemoteSocketAddress(), 0));
		}
	}

	private final void processGetOneCommand(Session session,
			Map<String, CachedData> values, Command executingCmd) {
		int mergeCount = executingCmd.getMergeCount();
		if (mergeCount < 0) {
			// single get
			if (values.get(executingCmd.getKey()) == null) {
				reconnect(session);
			} else {

				CachedData data = values.get(executingCmd.getKey());
				if (data != null)
					statistics(CommandType.GET_HIT);
				else
					statistics(CommandType.GET_MSS);
				executingCmd.setResult(data); // 设置CachedData返回，transcoder.decode
				// ()放到用户线程

				executingCmd.getLatch().countDown();
				executingCmd.setStatus(OperationStatus.DONE);
			}
		} else {
			// merge get
			List<Command> mergeCommands = executingCmd.getMergeCommands();
			executingCmd.getIoBuffer().free();
			for (Command nextCommand : mergeCommands) {
				CachedData data = values.get(nextCommand.getKey());
				nextCommand.setResult(data);
				if (data != null)
					statistics(CommandType.GET_HIT);
				else
					statistics(CommandType.GET_MSS);
				nextCommand.getLatch().countDown();
				nextCommand.setStatus(OperationStatus.DONE);
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void processGetManyCommand(Session session,
			Map<String, CachedData> values, Command executingCmd) {
		// 合并结果
		if (executingCmd.getCommandType() == Command.CommandType.GET_MANY) {
			statistics(CommandType.GET_MANY);
			Map result = (Map) executingCmd.getResult();
			Iterator<Map.Entry<String, CachedData>> it = values.entrySet()
					.iterator();
			while (it.hasNext()) {
				Map.Entry<String, CachedData> item = it.next();
				result.put(item.getKey(), executingCmd.getTranscoder().decode(
						item.getValue()));
			}

		} else {
			statistics(CommandType.GETS_MANY);
			Map result = (Map) executingCmd.getResult();
			Iterator<Map.Entry<String, CachedData>> it = values.entrySet()
					.iterator();
			while (it.hasNext()) {
				Map.Entry<String, CachedData> item = it.next();
				GetsResponse getsResult = new GetsResponse(item.getValue()
						.getCas(), executingCmd.getTranscoder().decode(
						item.getValue()));
				result.put(item.getKey(), getsResult);
			}
		}
		executingCmd.getLatch().countDown();
		executingCmd.setStatus(OperationStatus.DONE);
	}

	public final void statistics(Command.CommandType cmdType) {
		this.statisticsHandler.statistics(cmdType);
	}

	@SuppressWarnings("unchecked")
	public MemcachedHandler(Transcoder transcoder, XMemcachedClient client) {
		super();
		this.transcoder = transcoder;
		this.client = client;
		this.statisticsHandler = new StatisticsHandler();

	}

	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder() {
		return transcoder;
	}

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

}
