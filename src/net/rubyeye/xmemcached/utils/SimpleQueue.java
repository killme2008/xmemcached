package net.rubyeye.xmemcached.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import com.google.code.yanf4j.util.Queue;

/**
 * Simple queue. All methods are thread-safe.
 * 
 * @author dennis zhuang
 */
public class SimpleQueue<T> implements Queue<T> {

    private final List<T> list = new LinkedList<T>();
    private Lock lock;
    private Condition notEmpty,  notEnough;

    public SimpleQueue() {
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.notEnough = lock.newCondition();
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
                if (list.add(obj)) {
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
        if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            try {
                if (obj != null) {
                    if (list.add(obj)) {
                        if (this.notEmpty != null) {
                            notEmpty.signalAll();
                        }
                        if (this.list.size() >= getFetchCount()) {
                            notEnough.signalAll();
                        }
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
            while (isEmpty()) {
                notEmpty.await();
            }
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
        long start = System.currentTimeMillis();
        if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            try {
                long passed = System.currentTimeMillis() - start;
                if (timeout > passed) {
                    // 为空时阻塞
                    while (isEmpty()) {
                        if (!notEmpty.await(timeout - passed,
                                TimeUnit.MILLISECONDS)) {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
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
    private int fetchCount = Integer.MAX_VALUE;

    public int getFetchCount() {
        return fetchCount;
    }

    public void setFetchCount(int fetchCount) {
        this.fetchCount = fetchCount;
    }

    public List<T> fetch(int n) throws InterruptedException {
        if (n <= 0) {
            return null;
        }
        lock.lock();
        List<T> result = new ArrayList<T>(n);
        setFetchCount(n);
        try {
            while (this.list.size() < n) {
                notEnough.await();
            }
            for (int i = 0; i < n; i++) {
                result.add(list.remove(0));
            }
            setFetchCount(Integer.MAX_VALUE);
            return result;
        } finally {
            lock.unlock();
        }
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

    public void addFirst(T obj) throws InterruptedException {
        if (obj != null) {
            ((LinkedList) list).addFirst(obj);
            if (this.notEmpty != null) {
                notEmpty.signalAll();
            }

        }
    }
}