package net.rubyeye.xmemcached;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import net.rubyeye.xmemcached.command.Command;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.HandlerAdapter;

public class MemcachedHandler extends HandlerAdapter<Command> {

	private Transcoder transcoder;

	private static final int DEFAULT_COMMANDS_QUEUE_LEN = 16 * 1024;

	protected BlockingQueue<Command> executingCmds = new ArrayBlockingQueue<Command>(
			DEFAULT_COMMANDS_QUEUE_LEN);

	@Override
	public void onMessageSent(Session session, Command t) {
		try {
			executingCmds.put(t);
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void onReceive(Session session, final Command recvCmd) {
		try {
			final Command executingCmd = executingCmds.take();
			if (executingCmd == null)
				return;
			if (recvCmd.getException() != null) {
				executingCmd.setException(recvCmd.getException());
				executingCmd.getLatch().countDown();
			} else {

				switch (executingCmd.getCommandType()) {
				case EXCEPTION:

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

	private void processGetOneCommand(Command recvCmd, Command executingCmd) {
		if (recvCmd.getKey() == null) {
			executingCmd.setResult(null);
		} else {
			executingCmd.setResult(transcoder
					.decode(((List<CachedData>) recvCmd.getResult()).get(0)));
		}
		executingCmd.getLatch().countDown();
	}

	public MemcachedHandler(Transcoder transcoder) {
		super();
		this.transcoder = transcoder;
	}

}
