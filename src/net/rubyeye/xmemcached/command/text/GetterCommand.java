package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.google.code.yanf4j.nio.Session;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.AbstractCommand;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * get/gets command
 *
 * @author dennis
 *
 */
public class GetterCommand extends AbstractCommand {
	private byte[] cmd;

	public GetterCommand(byte[] key, byte[] cmd, CommandType cmdType) {
		super();
		this.key = key;
		this.cmd = cmd;
		this.commandType = cmdType;
	}

	private final void processGetOneCommand(Session session,
			Map<String, CachedData> values) {
		int mergeCount = this.mergeCount;
		if (mergeCount < 0) {
			// single get
			if (values.get(getKey()) == null) {
				session.close();
				return;
			} else {

				CachedData data = values.get(getKey());
				// TODO statistics
				// if (data != null)
				// statistics(CommandType.GET_HIT);
				// else
				// statistics(CommandType.GET_MSS);
				setResult(data); // 设置CachedData返回，transcoder.decode
				// ()放到用户线程

				countDownLatch();
			}
		} else {
			// merge get
			List<Command> mergeCommands = getMergeCommands();
			getIoBuffer().free();
			for (Command nextCommand : mergeCommands) {
				CachedData data = values.get(nextCommand.getKey());
				nextCommand.setResult(data);
				// TODO statistics
				// if (data != null)
				// statistics(CommandType.GET_HIT);
				// else
				// statistics(CommandType.GET_MSS);
				nextCommand.countDownLatch();
			}
		}

	}

	@SuppressWarnings("unchecked")
	private final void processGetManyCommand(final Session session,
			final Map<String, CachedData> values) {
		((List<Map>) result).add(values);
		this.countDownLatch();
	}

	@Override
	public boolean decode(ByteBuffer buffer, MemcachedTCPSession session) {
		while (true) {
			int origLimit = buffer.limit();
			int origPos = buffer.position();
			MemcachedHandler.nextLine(session, buffer);
			if (session.getCurrentLine() == null) {
				return false;
			}
			if (session.getCurrentLine().equals("END")) {
				if (this.commandType == CommandType.GET_MANY
						|| this.commandType == CommandType.GETS_MANY) {
					processGetManyCommand(session, session.currentValues);
				} else if (this.commandType == CommandType.GET_ONE
						|| this.commandType == CommandType.GETS_ONE) {
					processGetOneCommand(session, session.currentValues);
				} else {
					session.close();
					return false;
				}
				session.currentValues = null;
				session.resetStatus();
				return true;
			} else if (session.getCurrentLine().startsWith("VALUE")) {
				String[] items = MemcachedHandler.SPACE_SPLIT_PATTERN.split(
						session.getCurrentLine(), 5);
				int flag = Integer.parseInt(items[2]);
				int dataLen = Integer.parseInt(items[3]);
				// 不够数据，返回
				if (buffer.remaining() < dataLen + 2) {
					buffer.position(origPos).limit(origLimit);
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
				buffer.position(buffer.position()
						+ MemcachedHandler.SPLIT.remaining());
				session.setCurrentLine(null);
			} else {
				buffer.position(origPos).limit(origLimit);
				session.setCurrentLine(null);
				return false;
			}

		}
	}

	@Override
	public void encode(BufferAllocator allocator) {
		this.ioBuffer = allocator.allocate(cmd.length
				+ CommandFactory.CRLF.length + 1 + key.length);
		ByteUtils.setArguments(this.ioBuffer, cmd, key);
		this.ioBuffer.flip();
	}

}
