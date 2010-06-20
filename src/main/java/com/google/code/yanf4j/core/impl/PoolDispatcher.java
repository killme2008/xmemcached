package com.google.code.yanf4j.core.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.util.WorkerThreadFactory;

/**
 * 
 * 
 * 
 * Pool dispatcher,wrap a threadpool.
 * 
 * @author dennis
 * 
 */
public class PoolDispatcher implements Dispatcher {
	public static int POOL_QUEUE_SIZE_FACTOR = 1000;
	public static float MAX_POOL_SIZE_FACTOR = 1.25f;
	private ThreadPoolExecutor threadPool;

	public PoolDispatcher(int poolSize) {
		this(poolSize, 60, TimeUnit.SECONDS,
				new ThreadPoolExecutor.AbortPolicy());
	}

	public PoolDispatcher(int poolSize, long keepAliveTime, TimeUnit unit,
			RejectedExecutionHandler rejectedExecutionHandler) {
		this.threadPool = new ThreadPoolExecutor(poolSize,
				(int) (MAX_POOL_SIZE_FACTOR * poolSize), keepAliveTime, unit,
				new ArrayBlockingQueue<Runnable>(poolSize
						* POOL_QUEUE_SIZE_FACTOR), new WorkerThreadFactory());
		this.threadPool.setRejectedExecutionHandler(rejectedExecutionHandler);
	}

	public final void dispatch(Runnable r) {
		if (!this.threadPool.isShutdown()) {
			this.threadPool.execute(r);
		}
	}

	public void stop() {
		this.threadPool.shutdown();
		try {
			this.threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
