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
			executingCmds.push(t);
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
					processGetOneCommand(recvCmd, executingCmd);
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
	private void processGetOneCommand(Command recvCmd, Command executingCmd) {
		if (recvCmd.getKey() == null) {
			executingCmd.setResult(null);
		} else {
			executingCmd.setResult(transcoder
					.decode(((List<CachedData>) recvCmd.getResult()).get(0)));
		}
		executingCmd.getLatch().countDown();
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
