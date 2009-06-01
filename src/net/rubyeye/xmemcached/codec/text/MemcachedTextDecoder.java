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
import net.rubyeye.xmemcached.command.CommandType;
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
 * 
 * @author dennis
 * 
 */
@SuppressWarnings("unchecked")
public class MemcachedTextDecoder implements Decoder<Command> {

	private static final Log log = LogFactory
			.getLog(MemcachedTextDecoder.class);

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
	public static final String nextLine(Session session, ByteBuffer buffer) {
		if (session.getAttribute(MemcachedTCPSession.CURRENT_LINE_ATTR) != null) {
			return (String) session
					.getAttribute(MemcachedTCPSession.CURRENT_LINE_ATTR);
		}

		/**
		 * 测试表明采用 Shift-And算法匹配 >BM算法匹配效率 > 朴素匹配 > KMP匹配，
		 * 如果你有更好的建议，请email给我(killme2008@gmail.com)
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
				String line = new String(bytes, "utf-8");
				session.setAttribute(MemcachedTCPSession.CURRENT_LINE_ATTR,
						line);
				return line;
			} catch (UnsupportedEncodingException e) {
				log.error(e, e);

			}

		}
		return null;

	}

	/**
	 * 返回boolean值并唤醒
	 * 
	 * @param result
	 * @return
	 */
	private final Command notifyBoolean(MemcachedTCPSession session,
			Boolean result, CommandType expectedCmdType,
			CommandType... otherExpectedCmdType) {
		final Command executingCmd = session.pollCurrentExecutingCommand();
		boolean isExpected = (executingCmd.getCommandType() == expectedCmdType);
		if (!isExpected) {
			for (CommandType cmdType : otherExpectedCmdType) {
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
		if (session.peekCurrentExecutingCommand().decode(session, buffer)) {
			return session.pollCurrentExecutingCommand();
		}
		LABEL: while (true) {
			ParseStatus status = (ParseStatus) session.getCurrentStatus();
			switch (status) {
			case NULL:
				nextLine(session, buffer);
				if (session.getCurrentLine() == null) {
					return null;
				}
				String currentLine = session.getCurrentLine();
				if (currentLine.startsWith("VALUE")) {
					session.setAttribute(
							MemcachedTCPSession.CURRENT_GET_VALUES,
							new HashMap<String, CachedData>());
					session.setAttribute(MemcachedTCPSession.PARSE_STATUS_ATTR,
							ParseStatus.GET);
				} else if (currentLine.equals("STORED")) {
					return notifyBoolean(session, Boolean.TRUE,
							CommandType.SET, CommandType.CAS, CommandType.ADD,
							CommandType.REPLACE, CommandType.APPEND,
							CommandType.PREPEND);
				} else if (currentLine.equals("DELETED")) {
					return notifyBoolean(session, Boolean.TRUE,
							CommandType.DELETE);
				} else if (currentLine.equals("END")) {
					return parseEndCommand(session);
				} else if (currentLine.equals("EXISTS")) {
					return notifyBoolean(session, Boolean.FALSE,
							CommandType.CAS);
				} else if (currentLine.equals("NOT_STORED")) {
					return notifyBoolean(session, Boolean.FALSE,
							CommandType.SET, CommandType.ADD,
							CommandType.REPLACE, CommandType.APPEND,
							CommandType.PREPEND);
				} else if (currentLine.equals("NOT_FOUND")) {
					return notifyBoolean(session, Boolean.FALSE,
							CommandType.DELETE, CommandType.CAS,
							CommandType.INCR, CommandType.DECR);
				} else if (currentLine.equals("ERROR")) {
					return parseException(session);
				} else if (currentLine.startsWith("CLIENT_ERROR")) {
					return parseClientException(session, currentLine);
				} else if (currentLine.startsWith("SERVER_ERROR")) {
					return parseServerException(session, currentLine);
				} else if (currentLine.startsWith("VERSION ")) {
					return parseVersionCommand(session, currentLine);
				} else if (currentLine.equals("OK")) {
					return notifyBoolean(session, true, CommandType.FLUSH_ALL);
				} else if (currentLine.startsWith("STAT")) {
					session.setAttribute(MemcachedTCPSession.PARSE_STATUS_ATTR,
							ParseStatus.STATS);
				} else {
					return parseIncrDecrCommand(session, currentLine);
				}
				if (session.getCurrentStatus() != ParseStatus.NULL) {
					continue LABEL;
				} else {
					log.error("unknow response:" + currentLine);
					throw new IllegalStateException("unknown response:"
							+ session.getCurrentLine());
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
		while ((line = session.getCurrentLine()) != null) {
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
			session.removeCurrentLine();
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
			String currentLine = session.getCurrentLine();
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
				if (executingCommand.getCommandType() == CommandType.GET_MANY
						|| executingCommand.getCommandType() == CommandType.GETS_MANY) {
					processGetManyCommand(session, session
							.getCurrentParseGetValues(), executingCommand);
				} else if (executingCommand.getCommandType() == CommandType.GET_ONE
						|| executingCommand.getCommandType() == CommandType.GETS_ONE) {
					processGetOneCommand(session, session
							.getCurrentParseGetValues(), executingCommand);
				} else {
					session.close();
					return null;
				}
				session.removeCurrentParseGetValues();
				session.removeCurrentKey();
				session.resetStatus();
				return executingCommand;
			} else if (currentLine.startsWith("VALUE")) {
				if (session.getCurrentKey() == null) {
					StringTokenizer stringTokenizer = new StringTokenizer(
							currentLine, " ");
					stringTokenizer.nextToken();
					String currentKey = stringTokenizer.nextToken();
					session.setAttribute(MemcachedTCPSession.CURRENT_GET_KEY,
							currentKey);

					int flag = Integer.parseInt(stringTokenizer.nextToken());
					int dataLen = Integer.parseInt(stringTokenizer.nextToken());
					// maybe gets,it have cas value
					CachedData value = new CachedData(flag, null, dataLen, -1);
					if (stringTokenizer.hasMoreTokens()) {
						value.setCas(Long
								.parseLong(stringTokenizer.nextToken()));
					}
					session.getCurrentParseGetValues().put(currentKey, value);
				}

				CachedData value = session.getCurrentParseGetValues().get(
						session.getCurrentKey());
				// 不够数据，返回
				if (buffer.remaining() < value.getDataLen() + 2) {
					return null;
				}

				byte[] data = new byte[value.getDataLen()];
				buffer.get(data);
				value.setData(data);
				buffer.position(buffer.position() + SPLIT.remaining());
				session.removeCurrentLine();
				session.removeCurrentKey();
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
		if (executingCmd.getCommandType() == CommandType.GET_MANY) {
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
		CommandType cmdType = executingCmd.getCommandType();
		if (cmdType != CommandType.GET_ONE && cmdType != CommandType.GETS_ONE
				&& cmdType != CommandType.GET_MANY
				&& cmdType != CommandType.GETS_MANY) {
			session.close();
			return null;
		}
		if (executingCmd.getCommandType() == CommandType.GET_ONE)
			statistics(CommandType.GET_MSS);
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
		if (executingCmd.getCommandType() == CommandType.GET_ONE)
			statistics(CommandType.GET_MSS);
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
		if (executingCmd.getCommandType() == CommandType.GET_ONE)
			statistics(CommandType.GET_MSS);
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
		if (executingCmd.getCommandType() != CommandType.VERSION) {
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
		if (executingCmd.getCommandType() != CommandType.INCR
				&& executingCmd.getCommandType() != CommandType.DECR) {
			session.close();
			return null;
		}
		executingCmd.setResult(result);
		session.resetStatus();

		return executingCmd;

	}

	private final void statistics(CommandType cmdType) {
		this.statisticsHandler.statistics(cmdType);
	}

	public final Transcoder getTranscoder() {
		return transcoder;
	}

	public final void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

}
