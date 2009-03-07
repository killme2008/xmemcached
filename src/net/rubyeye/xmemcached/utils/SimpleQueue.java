package net.rubyeye.xmemcached.utils;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * Simple queue. All methods are thread-safe.
 * 
 * @author dennis zhuang
 */
public class SimpleQueue<T> implements ExtendedQueue<T> {

	private final List<T> list = new LinkedList<T>();
	private Lock lock;

	public SimpleQueue() {
		this.lock = new ReentrantLock();
	}

	public int getLowWaterMark() {
		throw new UnsupportedOperationException();
	}

	public void setLowWaterMark(int lowWaterMark) {
		throw new UnsupportedOperationException();
	}

	public int getHighWaterMark() {
		throw new UnsupportedOperationException();
	}

	public void setHighWaterMark(int highWaterMark) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xlands.game.tcpserver.util.Queue#push(java.lang.Object)
	 */
	public boolean push(T obj) throws InterruptedException {
		lock.lock();
		try {
			if (obj != null) {
				return list.add(obj);
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	public boolean push(T obj, long timeout) throws InterruptedException {
		if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
			try {
				if (obj != null) {
					if (list.add(obj)) {
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
			if (!isEmpty()) {
				return list.remove(0);
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	public T pop(long timeout) throws InterruptedException {
		if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
			try {
				if (!isEmpty()) {
					T t = list.remove(0);
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
		return false;
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

	@Override
	public void addFirst(T obj) {
		lock.lock();
		try {
			if (obj != null) {
				((LinkedList) list).addFirst(obj);
			}
		} finally {
			lock.unlock();
		}
	}

}