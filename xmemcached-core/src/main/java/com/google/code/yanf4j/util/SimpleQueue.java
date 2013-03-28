package com.google.code.yanf4j.util;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Simple queue. All methods are thread-safe.
 * 
 * @author dennis zhuang
 */
public class SimpleQueue<T> extends java.util.AbstractQueue<T> {

	protected final LinkedList<T> list;

	public SimpleQueue(int initializeCapacity) {
		this.list = new LinkedList<T>();
	}

	public SimpleQueue() {
		this(100);
	}

	public synchronized boolean offer(T e) {
		return this.list.add(e);
	}

	public synchronized T peek() {
		return this.list.peek();
	}

	public synchronized T poll() {
		return this.list.poll();
	}

	@Override
	public Iterator<T> iterator() {
		return this.list.iterator();
	}

	@Override
	public int size() {
		return this.list.size();
	}

}
