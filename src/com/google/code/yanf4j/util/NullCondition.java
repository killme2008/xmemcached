package com.google.code.yanf4j.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class NullCondition implements Condition {

	public void await() throws InterruptedException {
	}

	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		return false;
	}

	public long awaitNanos(long nanosTimeout) throws InterruptedException {
		return 0;
	}

	public void awaitUninterruptibly() {

	}

	public boolean awaitUntil(Date deadline) throws InterruptedException {
		return false;
	}

	public void signal() {

	}

	public void signalAll() {

	}

}
