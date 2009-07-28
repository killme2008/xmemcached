package net.rubyeye.xmemcached.utils.hibernate;

import java.util.Arrays;
import java.util.Map;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.hibernate.memcached.LoggingMemcacheExceptionHandler;
import com.googlecode.hibernate.memcached.Memcache;
import com.googlecode.hibernate.memcached.MemcacheExceptionHandler;
import com.googlecode.hibernate.memcached.utils.StringUtils;

public class Xmemcache implements Memcache {
	private static final Logger log = LoggerFactory.getLogger(Xmemcache.class);
	private MemcacheExceptionHandler exceptionHandler = new LoggingMemcacheExceptionHandler();

	private final MemcachedClient memcachedClient;

	public Xmemcache(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}

	public Object get(String key) {
		try {
			log.debug("MemcachedClient.get({})", key);
			return this.memcachedClient.get(key);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnGet(key, e);
		}
		return null;
	}

	public Map<String, Object> getMulti(String... keys) {
		try {
			return this.memcachedClient.get(Arrays.asList(keys));
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnGet(
					StringUtils.join(keys, ", "), e);
		}
		return null;
	}

	public void set(String key, int cacheTimeSeconds, Object o) {
		log.debug("MemcachedClient.set({})", key);
		try {
			this.memcachedClient.set(key, cacheTimeSeconds, o);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnSet(key, cacheTimeSeconds, o, e);
		}
	}

	public void delete(String key) {
		try {
			this.memcachedClient.delete(key);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnDelete(key, e);
		}
	}

	public void incr(String key, int factor, int startingValue) {
		try {
			this.memcachedClient.incr(key, factor);
		} catch (MemcachedException e) {
			try {
				this.memcachedClient.add(key, 0, String.valueOf(startingValue));
			} catch (Exception ex) {
				this.exceptionHandler.handleErrorOnIncr(key, factor,
						startingValue, ex);
			}
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnIncr(key, factor, startingValue,
					e);
		}
	}

	public void shutdown() {
		log.debug("Shutting down spy MemcachedClient");
		try {
			this.memcachedClient.shutdown();
		} catch (Exception e) {
			log.error("Shut down MemcachedClient error", e);
		}

	}

	public void setExceptionHandler(MemcacheExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
}
