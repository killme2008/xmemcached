package net.rubyeye.xmemcached;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import net.rubyeye.xmemcached.command.Command;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.Dispatcher;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.util.DispatcherFactory;

public class MemcachedHandler extends HandlerAdapter<Command> {

	private Transcoder transcoder;

	private Dispatcher dispatcher;

	protected LinkedBlockingQueue<Command> executingCmds = new LinkedBlockingQueue<Command>();

	@Override
	public void onException(Session session, Throwable t) {
		super.onException(session, t);
	}

	@Override
	public void onMessageSent(Session session, Command t) {
		if (t != null)
			executingCmds.add(t);
	}

	@Override
	public void onReceive(Session session, final Command recvCmd) {
		final Command executingCmd = executingCmds.poll();
		dispatcher.dispatch(new Runnable() {
			public void run() {
				if (executingCmd == null)
					return;
				switch (executingCmd.getCommandType()) {
				case EXCEPTION:
					
				case GET_ONE:
					processGetOneCommand(recvCmd, executingCmd);
					break;
					
				case SET:
				case ADD:
				case REPLACE:
					processSetCommand(recvCmd, executingCmd);
					break;
				}

			}
		});

	}

	private void processSetCommand(Command recvCmd, Command executingCmd) {
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

	@Override
	public void onSessionStarted(Session session) {
		super.onSessionStarted(session);
	}

	public MemcachedHandler(Transcoder transcoder) {
		super();
		this.transcoder = transcoder;
		this.dispatcher = DispatcherFactory.newDispatcher(Runtime.getRuntime()
				.availableProcessors());
	}

}
