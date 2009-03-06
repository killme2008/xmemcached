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
import java.util.LinkedList;
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

	/**
	 * 协议解析部分
	 */
	private ParseStatus status = ParseStatus.NULL; // 当前状态
	private static final ByteBuffer SPLIT = ByteBuffer.wrap(Command.SPLIT
			.getBytes());
	private String currentLine = null; // 当前行
	List<String> keys; // get获取的key列表
	List<CachedData> datas; // get获取的值列表
	/**
	 * BM算法匹配器，用于匹配行
	 */
	static ByteBufferMatcher SPLIT_MATCHER = new ByteBufferMatcher(SPLIT);

	/**
	 * 获取当前执行command
	 * 
	 * @return
	 */
	private Command getCurrentExecutingCommand() {
		return executingCmds.remove(0);
	}

	/**
	 * 返回boolean值并唤醒
	 * 
	 * @param result
	 * @return
	 */
	private boolean notifyBoolean(Boolean result) {
		Command executingCmd = getCurrentExecutingCommand();
		if (executingCmd == null) {
			return false;
		} else {
			executingCmd.setResult(result);
			executingCmd.getLatch().countDown();
			reset();
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

	public boolean onReceive(ByteBuffer buffer) {
		int origPos = buffer.position();
		int origLimit = buffer.limit();
		LABEL: while (true) {
			switch (this.status) {
			case NULL:
				nextLine(buffer);
				if (currentLine == null) {
					return false;
				}
				if (currentLine.startsWith("VALUE")) {
					this.keys = new ArrayList<String>(30);
					this.datas = new ArrayList<CachedData>(30);
					this.status = ParseStatus.GET;
				} else if (currentLine.equals("STORED")) {
					this.status = ParseStatus.STORED;
				} else if (currentLine.equals("DELETED")) {
					this.status = ParseStatus.DELETED;
				} else if (currentLine.equals("END")) {
					this.status = ParseStatus.END;
				} else if (currentLine.equals("NOT_STORED")) {
					this.status = ParseStatus.NOT_STORED;
				} else if (currentLine.equals("NOT_FOUND")) {
					this.status = ParseStatus.NOT_FOUND;
				} else if (currentLine.equals("ERROR")) {
					this.status = ParseStatus.ERROR;
				} else if (currentLine.startsWith("CLIENT_ERROR")) {
					this.status = ParseStatus.CLIENT_ERROR;
				} else if (currentLine.startsWith("SERVER_ERROR")) {
					this.status = ParseStatus.SERVER_ERROR;
				} else if (currentLine.startsWith("VERSION ")) {
					this.status = ParseStatus.VERSION;
				} else {
					this.status = ParseStatus.INCR;
				}
				if (!this.status.equals(ParseStatus.NULL)) {
					continue LABEL;
				} else {
					log.error("unknow response:" + this.currentLine);
					throw new IllegalStateException("unknown response:"
							+ this.currentLine);
				}
			case GET:
				return parseGet(buffer, origPos, origLimit);
			case END:
				return parseEndCommand();
			case STORED:
				return parseStored();
			case NOT_STORED:
				return parseNotStored();
			case DELETED:
				return parseDeleted();
			case NOT_FOUND:
				return parseNotFound();
			case ERROR:
				return parseException();
			case CLIENT_ERROR:
				return parseClientException();
			case SERVER_ERROR:
				return parseServerException();
			case VERSION:
				return parseVersionCommand();
			case INCR:
				return parseIncrDecrCommand();
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
	private boolean parseGet(ByteBuffer buffer, int origPos, int origLimit) {
		while (true) {
			nextLine(buffer);
			if (currentLine == null) {
				return false;
			}
			if (this.currentLine.equals("END")) {
				Command executingCommand = getCurrentExecutingCommand();
				if (executingCommand == null) {
					return false;
				}
				if (executingCommand.getCommandType().equals(
						Command.CommandType.GET_MANY)) {
					processGetManyCommand(keys, datas, executingCommand);
				} else {
					processGetOneCommand(keys, datas, executingCommand);
				}
				this.keys = null;
				this.datas = null;
				reset();
				return true;
			} else if (currentLine.startsWith("VALUE")) {
				String[] items = this.currentLine.split(" ");
				int flag = Integer.parseInt(items[2]);
				int dataLen = Integer.parseInt(items[3]);
				// 不够数据，返回
				if (buffer.remaining() < dataLen + 2) {
					buffer.position(origPos).limit(origLimit);
					this.currentLine = null;
					return false;
				}
				keys.add(items[1]);
				byte[] data = new byte[dataLen];
				buffer.get(data);
				datas.add(new CachedData(flag, data, dataLen));
				buffer.position(buffer.position() + SPLIT.remaining());
				this.currentLine = null;
			} else {
				buffer.position(origPos).limit(origLimit);
				this.currentLine = null;
				return false;
			}

		}
	}

	/**
	 * 解析get协议返回空
	 * 
	 * @return
	 */
	private boolean parseEndCommand() {
		Command executingCmd = getCurrentExecutingCommand();
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
		executingCmd.setResult(null);
		executingCmd.getLatch().countDown();
		reset();
		return true;

	}

	/**
	 * 解析存储协议response
	 * 
	 * @return
	 */
	private boolean parseStored() {
		return notifyBoolean(Boolean.TRUE);
	}

	/**
	 * 解析存储协议response
	 * 
	 * @return
	 */
	private boolean parseNotStored() {
		return notifyBoolean(Boolean.FALSE);
	}

	/**
	 * 解析delete协议response
	 * 
	 * @return
	 */
	private boolean parseDeleted() {
		return notifyBoolean(Boolean.TRUE);
	}

	/**
	 * 解析delete和incr协议response
	 * 
	 * @return
	 */
	private boolean parseNotFound() {
		return notifyBoolean(Boolean.FALSE);
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private boolean parseException() {
		Command executingCmd = getCurrentExecutingCommand();
		final MemcachedException exception = new MemcachedException(
				"unknown command");
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		reset();
		return true;
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private boolean parseClientException() {
		String[] items = this.currentLine.split(" ");
		final String error = items.length > 1 ? items[1]
				: "unknown client error";
		Command executingCmd = getCurrentExecutingCommand();

		final MemcachedClientException exception = new MemcachedClientException(
				error);
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		reset();

		return true;
	}

	/**
	 * 解析错误response
	 * 
	 * @return
	 */
	private boolean parseServerException() {
		String[] items = this.currentLine.split(" ");
		final String error = items.length > 1 ? items[1]
				: "unknown server error";
		Command executingCmd = getCurrentExecutingCommand();
		final MemcachedServerException exception = new MemcachedServerException(
				error);
		executingCmd.setException(exception);
		executingCmd.getLatch().countDown();
		reset();
		return true;

	}

	/**
	 * 解析version协议response
	 * 
	 * @return
	 */
	private boolean parseVersionCommand() {
		String[] items = this.currentLine.split(" ");
		final String version = items.length > 1 ? items[1] : "unknown version";
		Command executingCmd = getCurrentExecutingCommand();
		executingCmd.setResult(version);
		executingCmd.getLatch().countDown();
		reset();
		return true;

	}

	private void reset() {
		this.status = ParseStatus.NULL;
		this.currentLine = null;
	}

	/**
	 * 解析incr,decr协议response
	 * 
	 * @return
	 */
	private boolean parseIncrDecrCommand() {
		final Integer result = Integer.parseInt(this.currentLine);
		Command executingCmd = getCurrentExecutingCommand();
		executingCmd.setResult(result);
		executingCmd.getLatch().countDown();
		reset();

		return true;

	}

	/**
	 * 获取下一行
	 * 
	 * @param buffer
	 */
	protected void nextLine(ByteBuffer buffer) {
		if (this.currentLine != null) {
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
				this.currentLine = new String(bytes, "utf-8");
			} catch (UnsupportedEncodingException e) {
			}

		} else {
			this.currentLine = null;
		}

	}

	/**
	 * HandlerAdapter实现，负责命令管理和派发
	 */
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;
	protected List<Command> executingCmds = new LinkedList<Command>(); // 存储已经发送的命令
	protected XMemcachedClient client;
	private static final int MAX_TRIES = 5; // 重连最大次数
	protected static final Log log = LogFactory.getLog(MemcachedHandler.class);
	private int connectTries = 0;

	@Override
	public void onMessageSent(Session session, Command t) {
		executingCmds.add(t);
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
		reconnect();
	}

	protected void reconnect() {
		if (!this.client.isShutdown()) {
			if (this.connectTries < MAX_TRIES) {
				try {
					this.connectTries++;
					log.warn("Try to reconnect the server,It had try "
							+ this.connectTries + " times");

					client.getConnector().reconnect();
					this.executingCmds.clear();
				} catch (IOException e) {
					log.error("reconnect error", e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processGetOneCommand(List<String> keys,
			List<CachedData> datas, Command executingCmd) {
		int mergeCount = executingCmd.getMergeCount();
		if (mergeCount < 0) {
			// single get
			if (!executingCmd.getKey().equals(keys.get(0))) {
				reconnect(); // error,reconnect

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
}
