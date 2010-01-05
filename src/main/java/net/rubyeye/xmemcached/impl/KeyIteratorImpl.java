package net.rubyeye.xmemcached.impl;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.KeyIterator;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.command.text.TextCacheDumpCommand;
import net.rubyeye.xmemcached.exception.MemcachedException;

import com.google.code.yanf4j.core.Session;

/**
 * Default key iterator implementation
 * 
 * @author dennis
 * 
 */
public class KeyIteratorImpl implements KeyIterator {
	private final LinkedList<Integer> itemNumbersList;
	private LinkedList<String> currentKeyList;
	private final MemcachedClient memcachedClient;
	private final InetSocketAddress inetSocketAddress;
	private long opTimeout=1000;

	public KeyIteratorImpl(LinkedList<Integer> itemNumbersList,
			MemcachedClient memcachedClient, InetSocketAddress inetSocketAddress) {
		super();
		this.itemNumbersList = itemNumbersList;
		this.memcachedClient = memcachedClient;
		this.inetSocketAddress = inetSocketAddress;
	}

	public final InetSocketAddress getServerAddress() {
		return this.inetSocketAddress;
	}
	
	
	public final void setOpTimeout(long opTimeout) {
		this.opTimeout = opTimeout;
	}

	public void close() {
		this.itemNumbersList.clear();
		this.currentKeyList.clear();
		this.currentKeyList = null;
	}

	public boolean hasNext() {
		return (this.itemNumbersList != null && !this.itemNumbersList.isEmpty())
				|| (this.currentKeyList != null && !this.currentKeyList
						.isEmpty());
	}

	@SuppressWarnings("unchecked")
	public String next() throws MemcachedException, TimeoutException,
			InterruptedException {
		if (!hasNext()) {
			throw new ArrayIndexOutOfBoundsException();
		}

		if (this.currentKeyList != null && !this.currentKeyList.isEmpty()) {
			return this.currentKeyList.remove();
		}

		int itemNumber = this.itemNumbersList.remove();
		CountDownLatch latch = new CountDownLatch(1);
		TextCacheDumpCommand textCacheDumpCommand = new TextCacheDumpCommand(
				latch, itemNumber);
		Queue<Session> sessions = this.memcachedClient.getConnector()
				.getSessionByAddress(this.inetSocketAddress);
		if (sessions == null | sessions.size() == 0) {
			throw new MemcachedException(
					"The memcached server is not connected,"
							+ this.inetSocketAddress);
		}
		Session session = sessions.peek();
		session.write(textCacheDumpCommand);
		if (!latch.await(this.opTimeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("stats cachedump timeout");
		}
		this.currentKeyList = (LinkedList<String>) textCacheDumpCommand
				.getResult();
		return next();
	}

}
