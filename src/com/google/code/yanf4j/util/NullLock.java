package com.google.code.yanf4j.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class NullLock implements Lock {

	public void lock() {

	}

	public void lockInterruptibly() throws InterruptedException {

	}

	public Condition newCondition() {
		return new NullCondition();
	}

	public boolean tryLock() {
		return false;
	}

	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		return false;
	}

	public void unlock() {

	}

}
