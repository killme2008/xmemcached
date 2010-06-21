package com.google.code.yanf4j.core;

import java.util.Queue;

import com.google.code.yanf4j.statistics.Statistics;

/**
 * Session configuration
 * 
 * @author dennis
 * 
 */
public class SessionConfig {
	public final Handler handler;
	public final CodecFactory codecFactory;
	public final Statistics statistics;
	public final Queue<WriteMessage> queue;
	public final Dispatcher dispatchMessageDispatcher;
	public final boolean handleReadWriteConcurrently;
	public final long sessionTimeout;
	public final long sessionIdelTimeout;

	public SessionConfig(Handler handler, CodecFactory codecFactory,
			Statistics statistics, Queue<WriteMessage> queue,
			Dispatcher dispatchMessageDispatcher,
			boolean handleReadWriteConcurrently, long sessionTimeout,
			long sessionIdelTimeout) {

		this.handler = handler;
		this.codecFactory = codecFactory;
		this.statistics = statistics;
		this.queue = queue;
		this.dispatchMessageDispatcher = dispatchMessageDispatcher;
		this.handleReadWriteConcurrently = handleReadWriteConcurrently;
		this.sessionTimeout = sessionTimeout;
		this.sessionIdelTimeout = sessionIdelTimeout;
	}
}
