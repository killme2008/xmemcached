/**
 *Copyright [2008] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import com.google.code.yanf4j.nio.Message;

/**
 * Simple queue. All methods are thread-safe.
 * 
 * @author dennis zhuang
 */
public class MessageQueue<T extends Message> implements Queue<T> {

	private static final int DEFAULT_WATER_MARK = 16 * 1024;

	private final List<T> list = new LinkedList<T>();

	private Lock lock;

	private volatile int highWaterMark;

	private volatile int lowWaterMark;

	int bytesSize = 0;

	private Condition notEmpty, notFull;

	public MessageQueue() {
		this.lock = new ReentrantLock();
		this.lowWaterMark = this.highWaterMark = -1;
		this.notEmpty = this.notFull = null;
	}

	public MessageQueue(Lock lock) {
		this.lock = lock;
		this.notEmpty = this.lock.newCondition();
		this.notFull = this.lock.newCondition();
		this.lowWaterMark = this.highWaterMark = -1;
	}

	public MessageQueue(Lock lock, int lowWaterMark, int highWaterMark) {
		// 启用流量控制，不允许使用NullLock
		if (lock instanceof NullLock)
			throw new IllegalArgumentException();
		this.lock = lock;
		this.notEmpty = this.lock.newCondition();
		this.notFull = this.lock.newCondition();
		if (lowWaterMark <= 0 || highWaterMark <= 0)
			throw new IllegalArgumentException();
		this.lowWaterMark = lowWaterMark;
		this.highWaterMark = highWaterMark;
	}

	public MessageQueue(Lock lock, boolean enableFlowControll) {
		this(lock);
		if (enableFlowControll) {
			this.lowWaterMark = this.highWaterMark = DEFAULT_WATER_MARK;
		}
	}

	public MessageQueue(boolean enableFlowControll) {
		this(new ReentrantLock());
		if (enableFlowControll) {
			this.lowWaterMark = this.highWaterMark = DEFAULT_WATER_MARK;
		}
	}

	public MessageQueue(int lowWaterMark, int highWaterMark) {
		this(new ReentrantLock());
		if (lowWaterMark <= 0 || highWaterMark <= 0)
			throw new IllegalArgumentException();
		this.lowWaterMark = lowWaterMark;
		this.highWaterMark = highWaterMark;
	}

	public int getLowWaterMark() {
		return lowWaterMark;
	}

	public void setLowWaterMark(int lowWaterMark) {
		this.lowWaterMark = lowWaterMark;
	}

	public int getHighWaterMark() {
		return highWaterMark;
	}

	public void setHighWaterMark(int highWaterMark) {
		this.highWaterMark = highWaterMark;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#push(java.lang.Object)
	 */
	public boolean push(T obj) throws InterruptedException {
		lock.lock();
		try {
			if (this.highWaterMark > 0 && this.notFull != null) {
				while (this.bytesSize >= this.highWaterMark)
					notFull.await();
			}
			if (obj != null) {
				if (list.add(obj)) {
					this.bytesSize += obj.getLength();
					if (this.notEmpty != null)
						notEmpty.signalAll();
					return true;
				}
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	public boolean push(T obj, long timeout) throws InterruptedException {
		long start = System.currentTimeMillis();
		if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
			try {
				// 等待锁的时间
				long passed = System.currentTimeMillis() - start;
				if (this.highWaterMark > 0 && this.notFull != null) {
					if (timeout > passed) {
						while (this.bytesSize >= this.highWaterMark) {
							if (!notFull.await(timeout - passed,
									TimeUnit.MILLISECONDS))
								return false;
						}
					} else
						return false;
				}
				if (obj != null) {
					if (list.add(obj)) {
						this.bytesSize += obj.getLength();
						if (this.notEmpty != null)
							notEmpty.signalAll();
						return true;
					}
				}
			} finally {
				lock.unlock();
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#pop()
	 */
	public T pop() throws InterruptedException {
		lock.lock();
		try {
			if (this.lowWaterMark > 0 && this.notEmpty != null) {
				// 为空时阻塞
				while (isEmpty())
					notEmpty.await();
			}
			if (!isEmpty()) {
				T t = list.remove(0);
				if (t != null) {
					this.bytesSize -= t.getLength();
					// 低于低水位，通知可以继续放入
					if (this.bytesSize < this.lowWaterMark && notFull != null)
						notFull.signalAll();
				}
				return t;
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	public T pop(long timeout) throws InterruptedException {
		long start = System.currentTimeMillis();
		if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
			try {
				long passed = System.currentTimeMillis() - start;
				if (notEmpty != null)
					if (timeout > passed) {
						// 为空时阻塞
						while (isEmpty())
							if (!notEmpty.await(timeout - passed,
									TimeUnit.MILLISECONDS))
								return null;
					} else
						return null;

				if (!isEmpty()) {
					T t = list.remove(0);
					if (t != null) {
						this.bytesSize -= t.getLength();
						// 低于低水位，通知可以继续放入
						if (this.lowWaterMark > 0
								&& this.bytesSize < this.lowWaterMark
								&& notFull != null)
							notFull.signalAll();
					}
					return t;
				}
			} finally {
				lock.unlock();
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#peek()
	 */
	public T peek() {
		lock.lock();
		try {
			if (!list.isEmpty()) {
				return list.get(0);
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#clear()
	 */
	public void clear() {
		lock.lock();
		try {
			list.clear();
			this.bytesSize = 0;
		} finally {
			lock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#isEmpty()
	 */
	public boolean isEmpty() {
		lock.lock();
		try {
			return list.isEmpty();
		} finally {
			lock.unlock();
		}
	}

	public boolean isFull() {
		lock.lock();
		try {
			return this.highWaterMark > 0
					&& this.bytesSize >= this.highWaterMark;
		} finally {
			lock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#size()
	 */
	public int size() {
		lock.lock();
		try {
			return list.size();
		} finally {
			lock.unlock();
		}

	}

	public String toString() {
		lock.lock();
		try {
			return list.toString();
		} finally {
			lock.unlock();
		}
	}

	public Lock getLock() {
		return lock;
	}

	@SuppressWarnings("unchecked")
	public T[] drainToArray() {
		lock.lock();
		try {
			T[] result = (T[]) list.toArray();
			list.clear();
			return result;
		} finally {
			lock.unlock();
		}
	}

}