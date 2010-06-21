package com.google.code.yanf4j.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory for worker thread
 * 
 * @author dennis
 * 
 */
public class WorkerThreadFactory implements ThreadFactory {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	public WorkerThreadFactory(ThreadGroup group, String prefix) {
		if (group == null) {
			SecurityManager s = System.getSecurityManager();
			this.group = (s != null) ? s.getThreadGroup() : Thread
					.currentThread().getThreadGroup();
		} else {
			this.group = group;
		}
		if (prefix == null) {
			this.namePrefix = "pool-" + poolNumber.getAndIncrement()
					+ "-thread-";
		} else {
			this.namePrefix = prefix + "-" + poolNumber.getAndIncrement()
					+ "-thread-";
		}
	}

	public WorkerThreadFactory() {
		this(null, null);
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(this.group, r, this.namePrefix
				+ this.threadNumber.getAndIncrement(), 0);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}

}
