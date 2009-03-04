package net.rubyeye.xmemcached;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.command.Command;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.HandlerAdapter;
import com.google.code.yanf4j.util.Queue;
import com.google.code.yanf4j.util.SimpleQueue;

public class MemcachedHandler extends HandlerAdapter<Command> {
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;

	protected Queue<Command> executingCmds = new SimpleQueue<Command>();

	@Override
	public void onMessageSent(Session session, Command t) {
		try {
			if (t.getMergeCommands() == null) {
				executingCmds.push(t);

			} else {
				List<Command> mergeCmds = t.getMergeCommands();
				for (Command cmd : mergeCmds) {
					executingCmds.push(cmd);
				}

			}
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void onReceive(Session session, final Command recvCmd) {
		try {
			final Command executingCmd = executingCmds.pop();

			if (executingCmd == null)
				return;
			if (recvCmd.getException() != null) {
				executingCmd.setException(recvCmd.getException());
				executingCmd.getLatch().countDown();
			} else {

				switch (executingCmd.getCommandType()) {
				case EXCEPTION:
				case GET_MANY:
					processGetManyCommand(recvCmd, executingCmd);
					break;
				case GET_ONE:
					processGetOneCommand(session, recvCmd, executingCmd);
					break;
				case SET:
				case ADD:
				case REPLACE:
				case DELETE:
				case INCR:
				case DECR:
				case VERSION:
					processCommand(recvCmd, executingCmd);
					break;
				}
			}
		} catch (InterruptedException e) {

		}

	}

	private void processCommand(Command recvCmd, Command executingCmd) {
		executingCmd.setResult(recvCmd.getResult());
		executingCmd.getLatch().countDown();
	}

	@SuppressWarnings("unchecked")
	private void processGetOneCommand(Session session, Command recvCmd,
			Command executingCmd) {
		int mergCount = executingCmd.getMergetCount();
		// 无数据
		if (recvCmd.getKey() == null) {
			if (mergCount < 0) {
				// 单个，未合并
				executingCmd.setResult(null);
				executingCmd.getLatch().countDown();
			} else {
				int i = 0;
				try {
					Command nextCommand = executingCmd;
					while (i < mergCount) {
						nextCommand.setResult(null);
						nextCommand.getLatch().countDown(); // 通知
						i++;
						if (i < mergCount)
							nextCommand = this.executingCmds.pop();
					}
				} catch (InterruptedException e) {

				}
			}
		} else {
			// 有缓存数据
			List<CachedData> datas = (List<CachedData>) recvCmd.getResult();
			List<String> keys = (List<String>) recvCmd.getKey();

			if (mergCount < 0) {
				// 未合并情况
				if (!executingCmd.getKey().equals(keys.get(0)))
					session.close(); // key不相符，关闭连接
				executingCmd.setResult(transcoder.decode(datas.get(0)));
				executingCmd.getLatch().countDown();
			} else {
				int i = 0;
				try {
					Command nextCommand = executingCmd;
					while (i < mergCount) {
						int index = keys.indexOf(nextCommand.getKey());
						if (index >= 0) {
							nextCommand.setResult(transcoder.decode(datas
									.get(index)));
						} else
							nextCommand.setResult(null);
						nextCommand.getLatch().countDown();
						i++;
						if (i < mergCount)
							nextCommand = this.executingCmds.pop();
					}
				} catch (InterruptedException e) {

				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void processGetManyCommand(Command recvCmd, Command executingCmd) {
		if (recvCmd.getKey() == null) {
			executingCmd.setResult(null);
		} else {
			List<CachedData> datas = (List<CachedData>) recvCmd.getResult();
			List<String> keys = (List<String>) recvCmd.getKey();
			Map<String, Object> result = new HashMap<String, Object>();
			int len = keys.size();
			for (int i = 0; i < len; i++) {
				result.put(keys.get(i), transcoder.decode(datas.get(i)));
			}
			executingCmd.setResult(result);
		}
		executingCmd.getLatch().countDown();
	}

	@SuppressWarnings("unchecked")
	public MemcachedHandler(Transcoder transcoder) {
		super();
		this.transcoder = transcoder;
	}

}
