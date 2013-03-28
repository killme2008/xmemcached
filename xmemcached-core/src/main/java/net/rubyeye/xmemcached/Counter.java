package net.rubyeye.xmemcached;

import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedException;

/**
 * Counter,encapsulate the incr/decr methods.
 * 
 * @author dennis
 * 
 */
public final class Counter {
	private final MemcachedClient memcachedClient;
	private final String key;
	private final long initialValue;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.key == null) ? 0 : this.key.hashCode());
		result = prime
				* result
				+ ((this.memcachedClient == null) ? 0 : this.memcachedClient
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Counter other = (Counter) obj;
		if (this.key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!this.key.equals(other.key)) {
			return false;
		}
		if (this.memcachedClient == null) {
			if (other.memcachedClient != null) {
				return false;
			}
		} else if (!this.memcachedClient.equals(other.memcachedClient)) {
			return false;
		}
		return true;
	}

	public final String getKey() {
		return this.key;
	}

	/**
	 * Get current value
	 * 
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public long get() throws MemcachedException, InterruptedException,
			TimeoutException {
		Object result = this.memcachedClient.get(this.key);
		if (result == null) {
			throw new MemcachedClientException("key is not existed.");
		} else {
			if (result instanceof Long)
				return (Long) result;
			else
				return Long.valueOf(((String) result).trim());
		}
	}

	/**
	 * Set counter's value to expected.
	 * 
	 * @param value
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public void set(long value) throws MemcachedException,
			InterruptedException, TimeoutException {
		this.memcachedClient.set(this.key, 0, String.valueOf(value));
	}

	public Counter(MemcachedClient memcachedClient, String key,
			long initialValue) {
		super();
		this.memcachedClient = memcachedClient;
		this.key = key;
		this.initialValue = initialValue;
		try {
			this.memcachedClient.add(key, 0, String.valueOf(this.initialValue));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new IllegalStateException("Initialize counter failed", e);
		}
	}

	/**
	 * Increase value by one
	 * 
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public long incrementAndGet() throws MemcachedException,
			InterruptedException, TimeoutException {
		return this.memcachedClient.incr(this.key, 1, this.initialValue);
	}

	/**
	 * Decrease value by one
	 * 
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public long decrementAndGet() throws MemcachedException,
			InterruptedException, TimeoutException {
		return this.memcachedClient.decr(this.key, 1, this.initialValue);
	}

	/**
	 * Add value and get the result
	 * 
	 * @param delta
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public long addAndGet(long delta) throws MemcachedException,
			InterruptedException, TimeoutException {
		if (delta >= 0) {
			return this.memcachedClient
					.incr(this.key, delta, this.initialValue);
		} else {
			return this.memcachedClient.decr(this.key, -delta,
					this.initialValue);
		}
	}

}
