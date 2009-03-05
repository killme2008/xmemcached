package net.rubyeye.xmemcached.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.code.yanf4j.util.NullLock;
import com.google.code.yanf4j.util.Queue;

public class UnLockQueue<T> implements Queue<T> {

    private Lock lock;
    private final List<T> list = new LinkedList<T>();

    public UnLockQueue() {
        this.lock = new NullLock();
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
        if (obj != null) {
            if (list.add(obj)) {
                return true;
            }
        }
        return false;
    }

    public boolean push(T obj, long timeout) throws InterruptedException {
        if (obj != null) {
            if (list.add(obj)) {
                return true;
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

        if (!isEmpty()) {
            return list.remove(0);
        } else {
            return null;
        }

    }

    public T pop(long timeout) throws InterruptedException {
        if (!isEmpty()) {
            T t = list.remove(0);
            return t;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xlands.game.tcpserver.util.Queue#peek()
     */
    public T peek() {
        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xlands.game.tcpserver.util.Queue#clear()
     */
    public void clear() {

        list.clear();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xlands.game.tcpserver.util.Queue#isEmpty()
     */
    public boolean isEmpty() {

        return list.isEmpty();

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

        return list.size();

    }

    public String toString() {
        return list.toString();
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

    @SuppressWarnings("unchecked")
    public T[] drainToArray() {
        T[] result = (T[]) list.toArray();
        list.clear();
        return result;

    }

    public void addFirst(T obj) throws InterruptedException {

        if (obj != null) {
            ((LinkedList) list).addFirst(obj);
        }


    }
}
