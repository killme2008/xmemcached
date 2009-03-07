package net.rubyeye.xmemcached;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.command.Command;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.HandlerAdapter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.rubyeye.xmemcached.utils.ByteBufferMatcher;

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
	 * BM算法匹配器，用于匹配行
	 */
	static ByteBufferMatcher SPLIT_MATCHER = new ByteBufferMatcher(SPLIT);

	/**
	 * 返回boolean值并唤醒
	 * 
	 * @param result
	 * @return
	 */
	private boolean notifyBoolean(MemcachedTCPSession session, Boolean result) {
		Command executingCmd = session.getCurrentExecutingCommand();
		if (executingCmd == null) {
			return false;
		} else {
			executingCmd.setResult(result);
			executingCmd.getLatch().countDown();
			session.resetStatus();
			return true;
		}
	}

	/**
	 * 解析状态
	 * 
	 * @author dennis
	 * 
	 */
	enum ParseStatus {

		NULL, GET, END, STORED, NOT_STORED, ERROR, CLIENT_ERROR, SERVER_ERROR, DELETED, NOT_FOUND, VERSION, INCR;
	}

	int count = 0;

	public boolean onReceive(MemcachedTCPSession session, ByteBuffer buffer) {
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
					session.keys = new ArrayList<String>(30);
					session.datas = new ArrayList<CachedData>(30);
					session.status = ParseStatus.GET;
				} else if (session.currentLine.equals("STORED")) {
					session.status = ParseStatus.STORED;
				} else if (session.currentLine.equals("DELETED")) {
					session.status = ParseStatus.DELETED;
				} else if (session.currentLine.equals("END")) {
					session.status = ParseStatus.END;
				} else if (session.currentLine.equals("NOT_STORED")) {
					session.status = ParseStatus.NOT_STORED;
				} else if (session.currentLine.equals("NOT_FOUND")) {
					session.status = ParseStatus.NOT_FOUND;
				} else if (session.currentLine.equals("ERROR")) {
					session.status = ParseStatus.ERROR;
				} else if (session.currentLine.startsWith("CLIENT_ERROR")) {
					session.status = ParseStatus.CLIENT_ERROR;
				} else if (session.currentLine.startsWith("SERVER_ERROR")) {
					session.status = ParseStatus.SERVER_ERROR;
				} else if (session.currentLine.startsWith("VERSION ")) {
					session.status = ParseStatus.VERSION;
				} else {
					session.status = ParseStatus.INCR;
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
			case END:
				return parseEndCommand(session);
			case STORED:
				return parseStored(session);
			case NOT_STORED:
				return parseNotStored(session);
			case DELETED:
				return parseDeleted(session);
			case NOT_FOUND:
				return parseNotFound(session);
			case ERROR:
				return parseException(session);
			case CLIENT_ERROR:
				return parseClientException(session);
			case SERVER_ERROR:
				return parseServerException(session);
			case VERSION:
				return parseVersionCommand(session);
			case INCR:
				return parseIncrDecrCommand(session);
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
	private boolean parseGet(MemcachedTCPSession session, ByteBuffer buffer,
			int origPos, int origLimit) {
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
				if (executingCommand.getCommandType().equals(
						Command.CommandType.GET_MANY)) {
					processGetManyCommand(session.keys, session.datas,
							executingCommand);
				} else {
					processGetOneCommand(session.keys, session.datas,
							executingCommand);
				}
				session.datas = null;
				session.keys = null;
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
				session.keys.add(items[1]);
				byte[] data = new byte[dataLen];
				buffer.get(data);
				session.datas.add(new CachedData(flag, data, dataLen));
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
	private boolean parseEndCommand(MemcachedTCPSession session) {
		Command executingCmd = session.getCurrentExecutingCommand();
		int mergCount = executingCmd.getMergeCount();
		if (mergCount < 0) {
			// single
			executingCmd.setResult(null);
			executingCmd.getLatch().countDown();
		} else {
			// merge get
			List<Command> mergeCommands = executingCmd.getMergeCommands();
			for (Command nextCommand : mergeCommands) {

				nextCommand.setResult(null);
				nextCommand.getLatch().countDown(); // notify

			}

		}
		session.resetStatus();
		return true;

	}

	/**
	 * 解析存储协议response
	 * 
	 * @return
	 */
	private boolean parseStored(MemcachedTCPSession session) {
		return notifyBoolean(session, Boolean.TRUE);
	}

	/**
	 * 解析存储协议response
	 * 
	 * @return
	 */
	private boolean parseNotStored(MemcachedTCPSession session) {
		return notifyBoolean(session, Boolean.FALSE);
	}

	/**
	 * 解析delete协议response
	 * 
	 * @return
	 */
	private boolean parseDeleted(MemcachedTCPSession session) {
		return notifyBoolean(session, Boolean.TRUE);
	}

	/**
	 * 解析delete和incr协议response
	 * 
	 * @return
	 */
	private boolean parseNotFound(MemcachedTCPSession session) {
		return notifyBoolean(session, Boolean.FALSE);
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private boolean parseException(MemcachedTCPSession session) {
		Command executingCmd = session.getCurrentExecutingCommand();
		final MemcachedException exception = new MemcachedException(
				"unknown command");
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		session.resetStatus();
		return true;
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private boolean parseClientException(MemcachedTCPSession session) {
		String[] items = session.currentLine.split(" ");
		final String error = items.length > 1 ? items[1]
				: "unknown client error";
		Command executingCmd = session.getCurrentExecutingCommand();

		final MemcachedClientException exception = new MemcachedClientException(
				error);
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		session.resetStatus();

		return true;
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private boolean parseServerException(MemcachedTCPSession session) {
		String[] items = session.currentLine.split(" ");
		final String error = items.length > 1 ? items[1]
				: "unknown server error";
		Command executingCmd = session.getCurrentExecutingCommand();
		final MemcachedServerException exception = new MemcachedServerException(
				error);
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		session.resetStatus();
		return true;

	}

	/**
	 * 解析version协议response
	 * 
	 * @return
	 */
	private boolean parseVersionCommand(MemcachedTCPSession session) {
		String[] items = session.currentLine.split(" ");
		final String version = items.length > 1 ? items[1] : "unknown version";
		Command executingCmd = session.getCurrentExecutingCommand();
		executingCmd.setResult(version);
		executingCmd.getLatch().countDown();
		session.resetStatus();
		return true;

	}

	/**
	 * 解析incr,decr协议response
	 * 
	 * @return
	 */
	private boolean parseIncrDecrCommand(MemcachedTCPSession session) {
		final Integer result = Integer.parseInt(session.currentLine);
		Command executingCmd = session.getCurrentExecutingCommand();
		executingCmd.setResult(result);
		executingCmd.getLatch().countDown();
		session.resetStatus();

		return true;

	}

	/**
	 * 获取下一行
	 * 
	 * @param buffer
	 */
	protected void nextLine(MemcachedTCPSession session, ByteBuffer buffer) {
		if (session.currentLine != null) {
			return;
		}

		int index = SPLIT_MATCHER.matchFirst(buffer);
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
	private static final int MAX_TRIES = 5; // 重连最大次数
	protected static final Log log = LogFactory.getLog(MemcachedHandler.class);
	private int connectTries = 0;

	@Override
	public void onMessageSent(Session session, Command t) {
		((MemcachedTCPSession) session).executingCmds.add(t);
	}

	@Override
	public void onSessionStarted(Session session) {
		// 启用阻塞读写，因为memcached通常跑在局域网内，网络状况良好，采用阻塞读写效率更好
		session.setUseBlockingWrite(true);
		session.setUseBlockingRead(true);
	}

	@Override
	public void onSessionClosed(Session session) {
		log.warn("session close");
		this.client.getConnector().removeSession((MemcachedTCPSession) session);
		reconnect(session);
	}

	protected void reconnect(Session session) {
		if (!this.client.isShutdown()) {
			this.client.getConnector().addConnectSocketAddress(
					session.getRemoteSocketAddress());
		}
	}

	@SuppressWarnings("unchecked")
	private void processGetOneCommand(List<String> keys,
			List<CachedData> datas, Command executingCmd) {
		int mergeCount = executingCmd.getMergeCount();
		if (mergeCount < 0) {
			// single get
			if (!executingCmd.getKey().equals(keys.get(0))) {
				// TODO
				reconnect(null); // error,reconnect

			}
			executingCmd.setResult(transcoder.decode(datas.get(0)));
			executingCmd.getLatch().countDown();
		} else {
			List<Command> mergeCommands = executingCmd.getMergeCommands();
			for (Command nextCommand : mergeCommands) {
				int index = keys.indexOf(nextCommand.getKey());
				if (index >= 0) {
					nextCommand.setResult(transcoder.decode(datas.get(index)));
				} else {
					nextCommand.setResult(null);
				}
				nextCommand.getLatch().countDown();
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void processGetManyCommand(List<String> keys,
			List<CachedData> datas, Command executingCmd) {
		Map<String, Object> result = new HashMap<String, Object>();
		int len = keys.size();
		for (int i = 0; i < len; i++) {
			result.put(keys.get(i), transcoder.decode(datas.get(i)));
		}

		executingCmd.setResult(result);
		executingCmd.getLatch().countDown();
	}

	@SuppressWarnings("unchecked")
	public MemcachedHandler(Transcoder transcoder, XMemcachedClient client) {
		super();
		this.transcoder = transcoder;
		this.client = client;
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
