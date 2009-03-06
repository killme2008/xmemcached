package net.rubyeye.xmemcached.utils;

import com.google.code.yanf4j.util.Queue;

public interface ExtendedQueue<T> extends Queue<T> {
	public void addFirst(T obj);
}
