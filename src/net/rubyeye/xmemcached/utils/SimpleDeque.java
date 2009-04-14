/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple Dueue. All methods are thread-safe.
 *
 * @author dennis zhuang
 */
public class SimpleDeque<T> implements
        Deque<T> {

    public SimpleDeque(int initialCapacity) {
        this.queue = new ArrayDeque<T>(initialCapacity);
    }

    public SimpleDeque() {
        this.queue = new ArrayDeque<T>();
    }
    private final ArrayDeque<T> queue;
    private final Lock lock = new ReentrantLock();

    @Override
    public final void addFirst(T obj) {
        lock.lock();
        try {
            queue.addFirst(obj);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean add(T obj, long timeout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getHighWaterMark() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final Lock getLock() {
        return lock;
    }

    @Override
    public int getLowWaterMark() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isFull() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHighWaterMark(int highWaterMark) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setLowWaterMark(int lowWaterMark) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final boolean add(T e) {
        lock.lock();
        try {
            return queue.add(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final T element() {
        lock.lock();
        try {
            return queue.element();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final boolean offer(T e) {
        lock.lock();
        try {
            return queue.offer(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final T peek() {
        lock.lock();
        try {
            return queue.peek();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final T poll() {
        lock.lock();
        try {
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final T remove() {
        lock.lock();
        try {
            return queue.remove();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        lock.lock();
        try {
            return queue.addAll(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final void clear() {
        lock.lock();
        try {
            queue.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final boolean contains(Object o) {
        lock.lock();
        try {
            return queue.contains(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        lock.lock();
        try {
            return queue.containsAll(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final boolean remove(Object o) {
        lock.lock();
        try {
            return queue.remove(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final boolean removeAll(Collection<?> c) {
        lock.lock();
        try {
            return queue.removeAll(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final boolean retainAll(Collection<?> c) {
        lock.lock();
        try {
            return queue.retainAll(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        lock.lock();
        try {
            return queue.toArray();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T removeLast() {
        lock.lock();
        try {
            return queue.removeLast();
        } finally {
            lock.unlock();
        }

    }
}