package net.rubyeye.xmemcached.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 基于LinkedList实现的伪BlockingQueue，非线程安全
 *
 * @author dennis
 *
 * @param <T>
 */
public class SimpleBlockingQueue<T> implements BlockingQueue<T> {

    private LinkedList<T> list = new LinkedList<T>();

    @Override
    public T element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T peek() {
        return list.peek();
    }

    @Override
    public T poll() {
        return list.poll();
    }

    @Override
    public T remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return this.list.addAll(c);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.list.retainAll(c);
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Object[] toArray() {
        return this.list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.list.toArray(a);
    }

    @Override
    public boolean add(T e) {
        return this.list.add(e);
    }

    @Override
    public boolean contains(Object o) {
        return this.list.contains(o);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(T e, long timeout, TimeUnit unit)
            throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(T e) {
        return this.list.add(e);
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(T e) throws InterruptedException {
        this.list.add(e);
    }

    @Override
    public int remainingCapacity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        return this.list.remove(o);
    }

    @Override
    public T take() throws InterruptedException {
        return this.list.pop();
    }
}
