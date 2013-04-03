package net.rubyeye.xmemcached;

import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.exception.MemcachedException;

/**
 * MemcachedClient callable when using namespace in xmemcached.For example:
 * 
 * <pre>
 *   memcachedClient.withNamespace(userId,new MemcachedClientCallable<Void>{
 *     public Void call(MemcachedClient client) throws MemcachedException,
 * 			InterruptedException, TimeoutException{
 *      client.set("username",0,username);
 *      client.set("email",0,email);
 *      return null;
 *     }
 *   }); 
 *   //invalidate all items under the namespace.
 *   memcachedClient.invalidateNamespace(userId);
 * </pre>
 * 
 * @author dennis<killme2008@gmail.com>
 * @see MemcachedClient#withNamespace(String, MemcachedClientCallable)
 * @since 1.4.2
 * @param <T>
 */
public interface MemcachedClientCallable<T> {
	public T call(MemcachedClient client) throws MemcachedException,
			InterruptedException, TimeoutException;
}
