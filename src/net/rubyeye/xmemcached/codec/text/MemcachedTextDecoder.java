package net.rubyeye.xmemcached.codec.text;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.TextCommandFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.Command.CommandType;
import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.StatisticsHandler;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.CodecFactory.Decoder;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;
/**
 * Memcached text protocol decoder
 * @author dennis
 *
 */
@SuppressWarnings("unchecked")
public class MemcachedTextDecoder implements Decoder<Command> {

	private static final Log log = LogFactory
			.getLog(MemcachedTextDecoder.class);

	public static final String PARSE_STATUS_ATTR = "parse_status";
	public static final String CURRENT_LINE_ATTR = "current_line";
	public static final String CURRENT_GET_VALUES = "current_values";
	public static final String CURRENT_GET_KEY = "current_key";

	public MemcachedTextDecoder(StatisticsHandler statisticsHandler,
			Transcoder transcoder) {
		super();
		this.statisticsHandler = statisticsHandler;
		this.transcoder = transcoder;
	}

	private Transcoder transcoder;

	private static final ByteBuffer SPLIT = ByteBuffer
			.wrap(TextCommandFactory.SPLIT.getBytes());

	private final StatisticsHandler statisticsHandler;

	/**
	 * shift-and algorithm for ByteBuffer's match
	 */
	private static final ByteBufferMatcher SPLIT_MATCHER = new ShiftAndByteBufferMatcher(
			SPLIT);

	/**
	 * 获取下一行
	 *
	 * @param buffer
	 */
	private static final void nextLine(Session session, ByteBuffer buffer) {
		if (session.getAttribute(CURRENT_LINE_ATTR) != null) {
			return;
		}

		/**
		 * 测试表明采用 Shift-And算法匹配 >BM算法匹配效率 > 朴素匹配 > KMP匹配， 如果你有更好的建议，请email给我(killme2008@gmail.com)
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
				session.setAttribute(CURRENT_LINE_ATTR, new String(bytes,
						"utf-8"));
			} catch (UnsupportedEncodingException e) {
			}

		}
	}

	/**
	 * 返回boolean值并唤醒
	 *
	 * @param result
	 * @return
	 */
	private final Command notifyBoolean(MemcachedTCPSession session,
			Boolean result, Command.CommandType expectedCmdType,
			Command.CommandType... otherExpectedCmdType) {
		final Command executingCmd = session.pollCurrentExecutingCommand();
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
			return null;
		}
		executingCmd.setResult(result);
		session.resetStatus();
		return executingCmd;
	}

	@Override
	public Command decode(ByteBuffer buffer, Session origSession) {
		MemcachedTCPSession session = (MemcachedTCPSession) origSession;
		LABEL: while (true) {
			ParseStatus status = (ParseStatus) session
					.getAttribute(PARSE_STATUS_ATTR);
			switch (status) {
			case NULL:
				nextLine(session, buffer);
				if (session.getAttribute(CURRENT_LINE_ATTR) == null) {
					return null;
				}
				String currentLine = (String) session
						.getAttribute(CURRENT_LINE_ATTR);
				if (currentLine.startsWith("VALUE")) {
					session.setAttribute(CURRENT_GET_VALUES,
							new HashMap<String, CachedData>());
					session.setAttribute(PARSE_STATUS_ATTR, ParseStatus.GET);
				} else if (currentLine.equals("STORED")) {
					return notifyBoolean(session, Boolean.TRUE,
							Command.CommandType.SET, Command.CommandType.CAS,
							Command.CommandType.ADD,
							Command.CommandType.REPLACE,
							Command.CommandType.APPEND,
							Command.CommandType.PREPEND);
				} else if (currentLine.equals("DELETED")) {
					return notifyBoolean(session, Boolean.TRUE,
							Command.CommandType.DELETE);
				} else if (currentLine.equals("END")) {
					return parseEndCommand(session);
				} else if (currentLine.equals("EXISTS")) {
					return notifyBoolean(session, Boolean.FALSE,
							Command.CommandType.CAS);
				} else if (currentLine.equals("NOT_STORED")) {
					return notifyBoolean(session, Boolean.FALSE,
							Command.CommandType.SET, Command.CommandType.ADD,
							Command.CommandType.REPLACE,
							Command.CommandType.APPEND,
							Command.CommandType.PREPEND);
				} else if (currentLine.equals("NOT_FOUND")) {
					return notifyBoolean(session, Boolean.FALSE,
							Command.CommandType.DELETE,
							Command.CommandType.CAS, Command.CommandType.INCR,
							Command.CommandType.DECR);
				} else if (currentLine.equals("ERROR")) {
					return parseException(session);
				} else if (currentLine.startsWith("CLIENT_ERROR")) {
					return parseClientException(session, currentLine);
				} else if (currentLine.startsWith("SERVER_ERROR")) {
					return parseServerException(session, currentLine);
				} else if (currentLine.startsWith("VERSION ")) {
					return parseVersionCommand(session, currentLine);
				} else if (currentLine.equals("OK")) {
					return notifyBoolean(session, true,
							Command.CommandType.FLUSH_ALL);
				} else if (currentLine.startsWith("STAT")) {
					session.setAttribute(PARSE_STATUS_ATTR, ParseStatus.STATS);
				} else {
					return parseIncrDecrCommand(session, currentLine);
				}
				if (session.getAttribute(PARSE_STATUS_ATTR) != ParseStatus.NULL) {
					continue LABEL;
				} else {
					log.error("unknow response:" + currentLine);
					throw new IllegalStateException("unknown response:"
							+ session.getAttribute(CURRENT_LINE_ATTR));
				}
			case GET:
				return parseGet(session, buffer);
			case STATS:
				return parseStatsCommand(session, buffer);
			default:
				return null;

			}
		}
	}

	/**
	 * 处理统计消息
	 *
	 * @param session
	 * @param buffer
	 * @return
	 */
	private final Command parseStatsCommand(final MemcachedTCPSession session,
			final ByteBuffer buffer) {
		String line = null;
		while ((line = (String) session.getAttribute(CURRENT_LINE_ATTR)) != null) {
			if (line.equals("END")) { // 到消息结尾
				Command executingCommand = session
						.pollCurrentExecutingCommand();
				session.resetStatus();
				return executingCommand;
			}
			String[] items = line.split(" ");
			Command executingCommand = session.peekCurrentExecutingCommand();
			((Map<String, String>) executingCommand.getResult()).put(items[1],
					items[2]);
			session.removeAttribute(CURRENT_LINE_ATTR);
			nextLine(session, buffer);
		}
		return null;
	}

	// static final Pattern SPACE_SPLIT_PATTERN = Pattern.compile("\\s");

	/**
	 * 解析get协议response
	 *
	 * @param buffer
	 * @param origPos
	 * @param origLimit
	 * @return
	 */
	private final Command parseGet(MemcachedTCPSession session,
			ByteBuffer buffer) {
		while (true) {
			nextLine(session, buffer);
			String currentLine = (String) session
					.getAttribute(CURRENT_LINE_ATTR);
			if (currentLine == null) {
				return null;
			}
			if (currentLine.equals("END")) {
				Command executingCommand = session
						.pollCurrentExecutingCommand();
				if (executingCommand == null) {
					session.close();
					return null;
				}
				if (executingCommand.getCommandType() == Command.CommandType.GET_MANY
						|| executingCommand.getCommandType() == Command.CommandType.GETS_MANY) {
					processGetManyCommand(session,
							(Map<String, CachedData>) session
									.getAttribute(CURRENT_GET_VALUES),
							executingCommand);
				} else if (executingCommand.getCommandType() == Command.CommandType.GET_ONE
						|| executingCommand.getCommandType() == Command.CommandType.GETS_ONE) {
					processGetOneCommand(session,
							(Map<String, CachedData>) session
									.getAttribute(CURRENT_GET_VALUES),
							executingCommand);
				} else {
					session.close();
					return null;
				}
				session.removeAttribute(CURRENT_GET_VALUES);
				session.removeAttribute(CURRENT_GET_KEY);
				session.resetStatus();
				return executingCommand;
			} else if (currentLine.startsWith("VALUE")) {
				if (session.getAttribute(CURRENT_GET_KEY) == null) {
					StringTokenizer stringTokenizer = new StringTokenizer(
							currentLine, " ");
					stringTokenizer.nextToken();
					String currentKey = stringTokenizer.nextToken();
					session.setAttribute(CURRENT_GET_KEY, currentKey);

					int flag = Integer.parseInt(stringTokenizer.nextToken());
					int dataLen = Integer.parseInt(stringTokenizer.nextToken());
					// maybe gets,it have cas value
					CachedData value = new CachedData(flag, null, dataLen, -1);
					if (stringTokenizer.hasMoreTokens()) {
						value.setCas(Long
								.parseLong(stringTokenizer.nextToken()));
					}
					((Map<String, CachedData>) (session
							.getAttribute(CURRENT_GET_VALUES))).put(currentKey,
							value);
				}

				CachedData value = ((Map<String, CachedData>) (session
						.getAttribute(CURRENT_GET_VALUES)))
						.get((String) session.getAttribute(CURRENT_GET_KEY));
				// 不够数据，返回
				if (buffer.remaining() < value.getDataLen() + 2) {
					return null;
				}

				byte[] data = new byte[value.getDataLen()];
				buffer.get(data);
				value.setData(data);
				buffer.position(buffer.position() + SPLIT.remaining());
				session.removeAttribute(CURRENT_LINE_ATTR);
				session.removeAttribute(CURRENT_GET_KEY);
			} else {
				session.close();
				return null;
			}

		}
	}

	private final void processGetOneCommand(Session session,
			Map<String, CachedData> values, Command executingCmd) {
		int mergeCount = executingCmd.getMergeCount();
		if (mergeCount < 0) {
			// single get
			if (values.get(executingCmd.getKey()) == null) {
				session.close();
				return;
			} else {

				CachedData data = values.get(executingCmd.getKey());
				if (data != null)
					statistics(CommandType.GET_HIT);
				else
					statistics(CommandType.GET_MSS);
				executingCmd.setResult(data);
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
				nextCommand.countDownLatch();
			}
		}

	}

	private final void processGetManyCommand(final Session session,
			final Map<String, CachedData> values, final Command executingCmd) {
		// 合并结果
		if (executingCmd.getCommandType() == Command.CommandType.GET_MANY) {
			statistics(CommandType.GET_MANY);
		} else {
			statistics(CommandType.GETS_MANY);
		}
		List<Map> result = (List<Map>) executingCmd.getResult();
		result.add(values);
	}

	/**
	 * 解析get协议返回空
	 *
	 * @return
	 */
	private final Command parseEndCommand(MemcachedTCPSession session) {
		Command executingCmd = session.pollCurrentExecutingCommand();
		Command.CommandType cmdType = executingCmd.getCommandType();
		if (cmdType != Command.CommandType.GET_ONE
				&& cmdType != Command.CommandType.GETS_ONE
				&& cmdType != Command.CommandType.GET_MANY
				&& cmdType != Command.CommandType.GETS_MANY) {
			session.close();
			return null;
		}
		if (executingCmd.getCommandType() == Command.CommandType.GET_ONE)
			statistics(Command.CommandType.GET_MSS);
		else
			statistics(cmdType);
		if (executingCmd.getMergeCount() > 0) {
			// merge get
			List<Command> mergeCommands = executingCmd.getMergeCommands();
			for (Command nextCommand : mergeCommands) {
				nextCommand.countDownLatch();
			}

		}
		session.resetStatus();
		return executingCmd;

	}

	/**
	 * 解析错误response
	 *
	 * @return
	 */
	private static final Command parseException(MemcachedTCPSession session) {
		Command executingCmd = session.pollCurrentExecutingCommand();

		final UnknownCommandException exception = new UnknownCommandException(
				"Unknown command:" + executingCmd.toString()
						+ ",please check your memcached version");
		executingCmd.setException(exception);
		return executingCmd;
	}

	/**
	 * 解析错误response
	 *
	 * @return
	 */
	private final Command parseClientException(MemcachedTCPSession session,
			String currentLine) {
		int index = currentLine.indexOf(" ");
		final String error = index > 0 ? currentLine.substring(index + 1)
				: "unknown client error";
		Command executingCmd = session.pollCurrentExecutingCommand();
		if (executingCmd.getCommandType() == Command.CommandType.GET_ONE)
			statistics(Command.CommandType.GET_MSS);
		final MemcachedClientException exception = new MemcachedClientException(
				error + ",command:" + executingCmd.toString());
		executingCmd.setException(exception);
		session.resetStatus();
		return executingCmd;
	}

	/**
	 * 解析错误response
	 *
	 * @return
	 */
	private final Command parseServerException(MemcachedTCPSession session,
			String currentLine) {
		int index = currentLine.indexOf(" ");
		final String error = index > 0 ? currentLine.substring(index + 1)
				: "unknown server error";
		Command executingCmd = session.pollCurrentExecutingCommand();
		if (executingCmd.getCommandType() == Command.CommandType.GET_ONE)
			statistics(Command.CommandType.GET_MSS);
		final MemcachedServerException exception = new MemcachedServerException(
				error + ",command:" + executingCmd.toString());
		executingCmd.setException(exception);
		session.resetStatus();
		return executingCmd;

	}

	/**
	 * 解析version协议response
	 *
	 * @return
	 */
	private final Command parseVersionCommand(MemcachedTCPSession session,
			String currentLine) {
		String[] items = currentLine.split(" ");
		final String version = items.length > 1 ? items[1] : "unknown version";
		Command executingCmd = session.pollCurrentExecutingCommand();
		if (executingCmd.getCommandType() != Command.CommandType.VERSION) {
			session.close();
			return null;
		}
		executingCmd.setResult(version);
		session.resetStatus();
		return executingCmd;

	}

	/**
	 * 解析incr,decr协议response
	 *
	 * @return
	 */
	private final Command parseIncrDecrCommand(MemcachedTCPSession session,
			String currentLine) {
		final Integer result = Integer.parseInt(currentLine);
		Command executingCmd = session.pollCurrentExecutingCommand();
		statistics(executingCmd.getCommandType());
		if (executingCmd.getCommandType() != Command.CommandType.INCR
				&& executingCmd.getCommandType() != Command.CommandType.DECR) {
			session.close();
			return null;
		}
		executingCmd.setResult(result);
		session.resetStatus();

		return executingCmd;

	}

	private final void statistics(Command.CommandType cmdType) {
		this.statisticsHandler.statistics(cmdType);
	}

	public final Transcoder getTranscoder() {
		return transcoder;
	}

	public final void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

}
