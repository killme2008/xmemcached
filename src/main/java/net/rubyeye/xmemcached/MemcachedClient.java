package net.rubyeye.xmemcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.networking.Connector;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.Protocol;

/**
 * The memcached client's interface
 * 
 * @author dennis
 * 
 */
public interface MemcachedClient {

	/**
	 * Default thread number for reading nio's receive buffer and dispatch
	 * commands.Recommend users to set it equal or less to the memcached
	 * server's number on linux platform,keep default on windows.Default is 0.
	 */
	public static final int DEFAULT_READ_THREAD_COUNT = 0;

	/**
	 * Default TCP keeplive option,which is true
	 */
	public static final boolean DEFAULT_TCP_KEEPLIVE = true;
	/**
	 * Default connect timeout,1 minutes
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60000;
	/**
	 * Default socket's send buffer size,8k
	 */
	public static final int DEFAULT_TCP_SEND_BUFF_SIZE = 32 * 1024;
	/**
	 * Disable nagle algorithm by default
	 * 
	 */
	public static final boolean DEFAULT_TCP_NO_DELAY = true;
	/**
	 * Default session read buffer size,16k
	 */
	public static final int DEFAULT_SESSION_READ_BUFF_SIZE = 128 * 1024;
	/**
	 * Default socket's receive buffer size,16k
	 */
	public static final int DEFAULT_TCP_RECV_BUFF_SIZE = 64 * 1024;
	/**
	 * Default operation timeout,if the operation is not returned in 5
	 * second,throw TimeoutException.
	 */
	public static final long DEFAULT_OP_TIMEOUT = 5000L;
	/**
	 * With java nio,there is only one connection to a memcached.In a high
	 * concurrent enviroment,you may want to pool memcached clients.But a
	 * xmemcached client has to start a reactor thread and some thread pools,if
	 * you create too many clients,the cost is very large. Xmemcached supports
	 * connection pool instreadof client pool.you can create more connections to
	 * one or more memcached servers,and these connections share the same
	 * reactor and thread pools,it will reduce the cost of system.Default pool
	 * size is 1.
	 */
	public static final int DEFAULT_CONNECTION_POOL_SIZE = 1;

	/**
	 * Default session idle timeout,if session is idle,xmemcached will do a
	 * heartbeat action to check if connection is alive.
	 */
	public static final int DEFAULT_SESSION_IDLE_TIMEOUT = 5000;

	/**
	 * Default heal session interval in milliseconds.
	 */
	public static final long DEFAULT_HEAL_SESSION_INTERVAL = 2000;

	int MAX_QUEUED_NOPS = 40000;
	int DYNAMIC_MAX_QUEUED_NOPS = (int) (MAX_QUEUED_NOPS * (Runtime
			.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0));

	/**
	 * Default max queued noreply operations number.It is calcuated dynamically
	 * based on your jvm maximum memory.
	 * 
	 * @since 1.3.8
	 */
	public static final int DEFAULT_MAX_QUEUED_NOPS = DYNAMIC_MAX_QUEUED_NOPS > MAX_QUEUED_NOPS ? MAX_QUEUED_NOPS
			: DYNAMIC_MAX_QUEUED_NOPS;

	/**
	 * Maximum number of timeout exception for close connection.
	 * 
	 * @since 1.4.0
	 */
	public static final int DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD = 1000;

	/**
	 * Set the merge factor,this factor determins how many 'get' commands would
	 * be merge to one multi-get command.default is 150
	 * 
	 * @param mergeFactor
	 */
	public void setMergeFactor(final int mergeFactor);

	/**
	 * Get the connect timeout
	 * 
	 */
	public long getConnectTimeout();

	/**
	 * Set the connect timeout,default is 1 minutes
	 * 
	 * @param connectTimeout
	 */
	public void setConnectTimeout(long connectTimeout);

	/**
	 * return the session manager
	 * 
	 * @return
	 */
	public Connector getConnector();

	/**
	 * Enable/Disable merge many get commands to one multi-get command.true is
	 * to enable it,false is to disable it.Default is true.Recommend users to
	 * enable it.
	 * 
	 * @param optimizeGet
	 */
	public void setOptimizeGet(final boolean optimizeGet);

	/**
	 * Enable/Disable merge many command's buffers to one big buffer fit
	 * socket's send buffer size.Default is true.Recommend true.
	 * 
	 * @param optimizeMergeBuffer
	 */
	public void setOptimizeMergeBuffer(final boolean optimizeMergeBuffer);

	/**
	 * @return
	 */
	public boolean isShutdown();

	/**
	 * Aadd a memcached server,the thread call this method will be blocked until
	 * the connecting operations completed(success or fail)
	 * 
	 * @param server
	 *            host string
	 * @param port
	 *            port number
	 */
	public void addServer(final String server, final int port)
			throws IOException;

	/**
	 * Add a memcached server,the thread call this method will be blocked until
	 * the connecting operations completed(success or fail)
	 * 
	 * @param inetSocketAddress
	 *            memcached server's socket address
	 */
	public void addServer(final InetSocketAddress inetSocketAddress)
			throws IOException;

	/**
	 * Add many memcached servers.You can call this method through JMX or
	 * program
	 * 
	 * @param host
	 *            String like [host1]:[port1] [host2]:[port2] ...
	 */
	public void addServer(String hostList) throws IOException;

	/**
	 * Get current server list.You can call this method through JMX or program
	 */
	public List<String> getServersDescription();

	/**
	 * Remove many memcached server
	 * 
	 * @param host
	 *            String like [host1]:[port1] [host2]:[port2] ...
	 */
	public void removeServer(String hostList);

	/**
	 * Set the nio's ByteBuffer Allocator,use SimpleBufferAllocator by default.
	 * 
	 * 
	 * @param bufferAllocator
	 * @return
	 */
	@Deprecated
	public void setBufferAllocator(final BufferAllocator bufferAllocator);

	/**
	 * Get value by key
	 * 
	 * @param <T>
	 * @param key
	 *            Key
	 * @param timeout
	 *            Operation timeout,if the method is not returned in this
	 *            time,throw TimeoutException
	 * @param transcoder
	 *            The value's transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> T get(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public <T> T get(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public <T> T get(final String key, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	public <T> T get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Just like get,But it return a GetsResponse,include cas value for cas
	 * update.
	 * 
	 * @param <T>
	 * @param key
	 *            key
	 * @param timeout
	 *            operation timeout
	 * @param transcoder
	 * 
	 * @return GetsResponse
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> GetsResponse<T> gets(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #gets(String, long, Transcoder)
	 * @param <T>
	 * @param key
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> GetsResponse<T> gets(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #gets(String, long, Transcoder)
	 * @param <T>
	 * @param key
	 * @param timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> GetsResponse<T> gets(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #gets(String, long, Transcoder)
	 * @param <T>
	 * @param key
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@SuppressWarnings("unchecked")
	public <T> GetsResponse<T> gets(final String key,
			final Transcoder transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Bulk get items
	 * 
	 * @param <T>
	 * @param keyCollections
	 *            key collection
	 * @param opTimeout
	 *            opTimeout
	 * @param transcoder
	 *            Value transcoder
	 * @return Exists items map
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, T> get(final Collection<String> keyCollections,
			final long opTimeout, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #get(Collection, long, Transcoder)
	 * @param <T>
	 * @param keyCollections
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, T> get(final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #get(Collection, long, Transcoder)
	 * @param <T>
	 * @param keyCollections
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, T> get(final Collection<String> keyCollections)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #get(Collection, long, Transcoder)
	 * @param <T>
	 * @param keyCollections
	 * @param timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, T> get(final Collection<String> keyCollections,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Bulk gets items
	 * 
	 * @param <T>
	 * @param keyCollections
	 *            key collection
	 * @param opTime
	 *            Operation timeout
	 * @param transcoder
	 *            Value transcoder
	 * @return Exists GetsResponse map
	 * @see net.rubyeye.xmemcached.GetsResponse
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long opTime,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #gets(Collection, long, Transcoder)
	 * @param <T>
	 * @param keyCollections
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #gets(Collection, long, Transcoder)
	 * @param <T>
	 * @param keyCollections
	 * @param timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #gets(Collection, long, Transcoder)
	 * @param <T>
	 * @param keyCollections
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Store key-value item to memcached
	 * 
	 * @param <T>
	 * @param key
	 *            stored key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 *            stored data
	 * @param transcoder
	 *            transocder
	 * @param timeout
	 *            operation timeout,in milliseconds
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean set(final String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #set(String, int, Object, Transcoder, long)
	 */
	public boolean set(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #set(String, int, Object, Transcoder, long)
	 */
	public boolean set(final String key, final int exp, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * @see #set(String, int, Object, Transcoder, long)
	 */
	public <T> boolean set(final String key, final int exp, final T value,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Store key-value item to memcached,doesn't wait for reply
	 * 
	 * @param <T>
	 * @param key
	 *            stored key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 *            stored data
	 * @param transcoder
	 *            transocder
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void setWithNoReply(final String key, final int exp,
			final Object value) throws InterruptedException, MemcachedException;

	/**
	 * @see #setWithNoReply(String, int, Object, Transcoder)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void setWithNoReply(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws InterruptedException, MemcachedException;

	/**
	 * Add key-value item to memcached, success only when the key is not exists
	 * in memcached.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean add(final String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #add(String, int, Object, Transcoder, long)
	 * @param key
	 * @param exp
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean add(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #add(String, int, Object, Transcoder, long)
	 * @param key
	 * @param exp
	 * @param value
	 * @param timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean add(final String key, final int exp, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * @see #add(String, int, Object, Transcoder, long)
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean add(final String key, final int exp, final T value,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Add key-value item to memcached, success only when the key is not exists
	 * in memcached.This method doesn't wait for reply.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */

	public void addWithNoReply(final String key, final int exp,
			final Object value) throws InterruptedException, MemcachedException;

	/**
	 * @see #addWithNoReply(String, int, Object, Transcoder)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void addWithNoReply(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws InterruptedException, MemcachedException;

	/**
	 * Replace the key's data item in memcached,success only when the key's data
	 * item is exists in memcached.This method will wait for reply from server.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean replace(final String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #replace(String, int, Object, Transcoder, long)
	 * @param key
	 * @param exp
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean replace(final String key, final int exp, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #replace(String, int, Object, Transcoder, long)
	 * @param key
	 * @param exp
	 * @param value
	 * @param timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean replace(final String key, final int exp, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * @see #replace(String, int, Object, Transcoder, long)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean replace(final String key, final int exp, final T value,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Replace the key's data item in memcached,success only when the key's data
	 * item is exists in memcached.This method doesn't wait for reply from
	 * server.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 * @param transcoder
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void replaceWithNoReply(final String key, final int exp,
			final Object value) throws InterruptedException, MemcachedException;

	/**
	 * @see #replaceWithNoReply(String, int, Object, Transcoder)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void replaceWithNoReply(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws InterruptedException, MemcachedException;

	/**
	 * @see #append(String, Object, long)
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Append value to key's data item,this method will wait for reply
	 * 
	 * @param key
	 * @param value
	 * @param timeout
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean append(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Append value to key's data item,this method doesn't wait for reply.
	 * 
	 * @param key
	 * @param value
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void appendWithNoReply(final String key, final Object value)
			throws InterruptedException, MemcachedException;

	/**
	 * @see #prepend(String, Object, long)
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean prepend(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Prepend value to key's data item in memcached.This method doesn't wait
	 * for reply.
	 * 
	 * @param key
	 * @param value
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean prepend(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Prepend value to key's data item in memcached.This method doesn't wait
	 * for reply.
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void prependWithNoReply(final String key, final Object value)
			throws InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, Object, Transcoder, long, long)
	 * @param key
	 * @param exp
	 * @param value
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean cas(final String key, final int exp, final Object value,
			final long cas) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Cas is a check and set operation which means "store this data but only if
	 * no one else has updated since I last fetched it."
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long timeout, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, Object, Transcoder, long, long)
	 * @param key
	 * @param exp
	 * @param value
	 * @param timeout
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean cas(final String key, final int exp, final Object value,
			final long timeout, final long cas) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, Object, Transcoder, long, long)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final int exp, final T value,
			final Transcoder<T> transcoder, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Cas is a check and set operation which means "store this data but only if
	 * no one else has updated since I last fetched it."
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param operation
	 *            CASOperation
	 * @param transcoder
	 *            object transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * cas is a check and set operation which means "store this data but only if
	 * no one else has updated since I last fetched it."
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            An expiration time, in seconds. Can be up to 30 days. After 30
	 *            days, is treated as a unix timestamp of an exact date.
	 * @param getsReponse
	 *            gets method's result
	 * @param operation
	 *            CASOperation
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, GetsResponse, CASOperation, Transcoder)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param getsReponse
	 * @param operation
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, GetsResponse, CASOperation, Transcoder)
	 * @param <T>
	 * @param key
	 * @param getsResponse
	 * @param operation
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, GetsResponse<T> getsResponse,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, GetsResponse, CASOperation, Transcoder)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param operation
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #cas(String, int, GetsResponse, CASOperation, Transcoder)
	 * @param <T>
	 * @param key
	 * @param operation
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> boolean cas(final String key, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 
	 * @param <T>
	 * @param key
	 * @param getsResponse
	 * @param operation
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void casWithNoReply(final String key,
			GetsResponse<T> getsResponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * cas noreply
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param getsReponse
	 * @param operation
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void casWithNoReply(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see #casWithNoReply(String, int, GetsResponse, CASOperation)
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param operation
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void casWithNoReply(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * @see #casWithNoReply(String, int, GetsResponse, CASOperation)
	 * @param <T>
	 * @param key
	 * @param operation
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> void casWithNoReply(final String key,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Delete key's data item from memcached.It it is not exists,return
	 * false.</br> time is the amount of time in seconds (or Unix time
	 * until</br> which) the client wishes the server to refuse "add" and
	 * "replace"</br> commands with this key. For this amount of item, the item
	 * is put into a</br> delete queue, which means that it won't possible to
	 * retrieve it by the</br> "get" command, but "add" and "replace" command
	 * with this key will also</br> fail (the "set" command will succeed,
	 * however). After the time passes,</br> the item is finally deleted from
	 * server memory. </br><strong>Note: This method is deprecated,because
	 * memcached 1.4.0 remove the optional argument "time".You can still use
	 * this method on old version,but is not recommended.</strong>
	 * 
	 * @param key
	 * @param time
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@Deprecated
	public boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Delete key's date item from memcached
	 * 
	 * @param key
	 * @param opTimeout
	 *            Operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 * @since 1.3.2
	 */
	public boolean delete(final String key, long opTimeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Set a new expiration time for an existing item
	 * 
	 * @param key
	 *            item's key
	 * @param exp
	 *            New expiration time, in seconds. Can be up to 30 days. After
	 *            30 days, is treated as a unix timestamp of an exact date.
	 * @param opTimeout
	 *            operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean touch(final String key, int exp, long opTimeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Set a new expiration time for an existing item,using default opTimeout
	 * second.
	 * 
	 * @param key
	 *            item's key
	 * @param exp
	 *            New expiration time, in seconds. Can be up to 30 days. After
	 *            30 days, is treated as a unix timestamp of an exact date.
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public boolean touch(final String key, int exp) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Get item and set a new expiration time for it
	 * 
	 * @param <T>
	 * @param key
	 *            item's key
	 * @param newExp
	 *            New expiration time, in seconds. Can be up to 30 days. After
	 *            30 days, is treated as a unix timestamp of an exact date.
	 * @param opTimeout
	 *            operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> T getAndTouch(final String key, int newExp, long opTimeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Get item and set a new expiration time for it,using default opTimeout
	 * 
	 * @param <T>
	 * @param key
	 * @param newExp
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public <T> T getAndTouch(final String key, int newExp)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Bulk get items and touch them
	 * 
	 * @param <T>
	 * @param keyExpMap
	 *            A map,key is item's key,and value is a new expiration time for
	 *            the item.
	 * @param opTimeout
	 *            operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	// public <T> Map<String, T> getAndTouch(Map<String, Integer> keyExpMap,
	// long opTimeout) throws TimeoutException, InterruptedException,
	// MemcachedException;

	/**
	 * Bulk get items and touch them,using default opTimeout
	 * 
	 * @see #getAndTouch(Map, long)
	 * @param <T>
	 * @param keyExpMap
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	// public <T> Map<String, T> getAndTouch(Map<String, Integer> keyExpMap)
	// throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Get all connected memcached servers's version.
	 * 
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public Map<InetSocketAddress, String> getVersions()
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @return the new value of the item's data, after the increment operation
	 *         was carried out.
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public long incr(final String key, final long delta)
			throws TimeoutException, InterruptedException, MemcachedException;

	public long incr(final String key, final long delta, final long initValue)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 *            key
	 * @param num
	 *            increment
	 * @param initValue
	 *            initValue if the data is not exists.
	 * @param timeout
	 *            operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public long incr(final String key, final long delta, final long initValue,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * "decr" are used to change data for some item in-place, decrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for decr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @return the new value of the item's data, after the decrement operation
	 *         was carried out.
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public long decr(final String key, final long delta)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see decr
	 * @param key
	 * @param num
	 * @param initValue
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public long decr(final String key, final long delta, long initValue)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "decr" are used to change data for some item in-place, decrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for decr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 *            The key
	 * @param num
	 *            The increment
	 * @param initValue
	 *            The initial value if the data is not exists.
	 * @param timeout
	 *            Operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public long decr(final String key, final long delta, long initValue,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Make All connected memcached's data item invalid
	 * 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void flushAll() throws TimeoutException, InterruptedException,
			MemcachedException;

	public void flushAllWithNoReply() throws InterruptedException,
			MemcachedException;

	/**
	 * Make All connected memcached's data item invalid
	 * 
	 * @param timeout
	 *            operation timeout
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Invalidate all existing items immediately
	 * 
	 * @param address
	 *            Target memcached server
	 * @param timeout
	 *            operation timeout
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void flushAll(InetSocketAddress address) throws MemcachedException,
			InterruptedException, TimeoutException;

	public void flushAllWithNoReply(InetSocketAddress address)
			throws MemcachedException, InterruptedException;

	public void flushAll(InetSocketAddress address, long timeout)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * This method is deprecated,please use flushAll(InetSocketAddress) instead.
	 * 
	 * @deprecated
	 * @param host
	 * 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@Deprecated
	public void flushAll(String host) throws TimeoutException,
			InterruptedException, MemcachedException;

	public Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * �ョ��瑰������emcached server缁��淇℃�
	 * 
	 * @param address
	 *            ����板�
	 * @param timeout
	 *            ���瓒��
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public Map<String, String> stats(InetSocketAddress address, long timeout)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Get stats from all memcached servers
	 * 
	 * @param timeout
	 * @return server->item->value map
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public Map<InetSocketAddress, Map<String, String>> getStats(long timeout)
			throws MemcachedException, InterruptedException, TimeoutException;

	public Map<InetSocketAddress, Map<String, String>> getStats()
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Get special item stats. "stats items" for example
	 * 
	 * @param item
	 * @return
	 */
	public Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName) throws MemcachedException, InterruptedException,
			TimeoutException;;

	public void shutdown() throws IOException;

	public boolean delete(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * return default transcoder,default is SerializingTranscoder
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder();

	/**
	 * set transcoder
	 * 
	 * @param transcoder
	 */
	@SuppressWarnings("unchecked")
	public void setTranscoder(final Transcoder transcoder);

	public Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName, long timeout) throws MemcachedException,
			InterruptedException, TimeoutException;

	/**
	 * get operation timeout setting
	 * 
	 * @return
	 */
	public long getOpTimeout();

	/**
	 * set operation timeout,default is one second.
	 * 
	 * @param opTimeout
	 */
	public void setOpTimeout(long opTimeout);

	public Map<InetSocketAddress, String> getVersions(long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Returns available memcached servers list.This method is drepcated,please
	 * use getAvailableServers instead.
	 * 
	 * @see #getAvailableServers()
	 * @return
	 */
	@Deprecated
	public Collection<InetSocketAddress> getAvaliableServers();

	/**
	 * Returns available memcached servers list.
	 * 
	 * @return A available server collection
	 */
	public Collection<InetSocketAddress> getAvailableServers();

	/**
	 * add a memcached server to MemcachedClient
	 * 
	 * @param server
	 * @param port
	 * @param weight
	 * @throws IOException
	 */
	public void addServer(final String server, final int port, int weight)
			throws IOException;

	public void addServer(final InetSocketAddress inetSocketAddress, int weight)
			throws IOException;

	/**
	 * Delete key's data item from memcached.This method doesn't wait for reply.
	 * This method does not work on memcached 1.3 or later version.See <a href=
	 * 'http://code.google.com/p/memcached/issues/detail?id=3&q=delete%20noreply
	 * ' > i s s u e 3</a> </br><strong>Note: This method is deprecated,because
	 * memcached 1.4.0 remove the optional argument "time".You can still use
	 * this method on old version,but is not recommended.</strong>
	 * 
	 * @param key
	 * @param time
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@Deprecated
	public void deleteWithNoReply(final String key, final int time)
			throws InterruptedException, MemcachedException;

	public void deleteWithNoReply(final String key)
			throws InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void incrWithNoReply(final String key, final long delta)
			throws InterruptedException, MemcachedException;

	/**
	 * "decr" are used to change data for some item in-place, decrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for decr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void decrWithNoReply(final String key, final long delta)
			throws InterruptedException, MemcachedException;

	/**
	 * Set the verbosity level of the memcached's logging output.This method
	 * will wait for reply.
	 * 
	 * @param address
	 * @param level
	 *            logging level
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void setLoggingLevelVerbosity(InetSocketAddress address, int level)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Set the verbosity level of the memcached's logging output.This method
	 * doesn't wait for reply from server
	 * 
	 * @param address
	 *            memcached server address
	 * @param level
	 *            logging level
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void setLoggingLevelVerbosityWithNoReply(InetSocketAddress address,
			int level) throws InterruptedException, MemcachedException;

	/**
	 * Add a memcached client listener
	 * 
	 * @param listener
	 */
	public void addStateListener(MemcachedClientStateListener listener);

	/**
	 * Remove a memcached client listener
	 * 
	 * @param listener
	 */
	public void removeStateListener(MemcachedClientStateListener listener);

	/**
	 * Get all current state listeners
	 * 
	 * @return
	 */
	public Collection<MemcachedClientStateListener> getStateListeners();

	public void flushAllWithNoReply(int exptime) throws InterruptedException,
			MemcachedException;

	public void flushAll(int exptime, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public void flushAllWithNoReply(InetSocketAddress address, int exptime)
			throws MemcachedException, InterruptedException;

	public void flushAll(InetSocketAddress address, long timeout, int exptime)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * If the memcached dump or network error cause connection closed,xmemcached
	 * would try to heal the connection.The interval between reconnections is 2
	 * seconds by default. You can change that value by this method.
	 * 
	 * @param healConnectionInterval
	 *            MILLISECONDS
	 */
	public void setHealSessionInterval(long healConnectionInterval);

	/**
	 * If the memcached dump or network error cause connection closed,xmemcached
	 * would try to heal the connection.You can disable this behaviour by using
	 * this method:<br/>
	 * <code> client.setEnableHealSession(false); </code><br/>
	 * The default value is true.
	 * 
	 * @param enableHealSession
	 * @since 1.3.9
	 */
	public void setEnableHealSession(boolean enableHealSession);

	/**
	 * Return the default heal session interval in milliseconds
	 * 
	 * @return
	 */
	public long getHealSessionInterval();

	public Protocol getProtocol();

	/**
	 * Store all primitive type as string,defualt is false.
	 */
	public void setPrimitiveAsString(boolean primitiveAsString);

	/**
	 * In a high concurrent enviroment,you may want to pool memcached
	 * clients.But a xmemcached client has to start a reactor thread and some
	 * thread pools,if you create too many clients,the cost is very large.
	 * Xmemcached supports connection pool instreadof client pool.you can create
	 * more connections to one or more memcached servers,and these connections
	 * share the same reactor and thread pools,it will reduce the cost of
	 * system.
	 * 
	 * @param poolSize
	 *            pool size,default is one,every memcached has only one
	 *            connection.
	 */
	public void setConnectionPoolSize(int poolSize);

	/**
	 * Whether to enable heart beat
	 * 
	 * @param enableHeartBeat
	 *            if true,then enable heartbeat,true by default
	 */
	public void setEnableHeartBeat(boolean enableHeartBeat);

	/**
	 * Enables/disables sanitizing keys by URLEncoding.
	 * 
	 * @param sanitizeKey
	 *            if true, then URLEncode all keys
	 */
	public void setSanitizeKeys(boolean sanitizeKey);

	public boolean isSanitizeKeys();

	/**
	 * Get counter for key,and if the key's value is not set,then set it with 0.
	 * 
	 * @param key
	 * @return
	 */
	public Counter getCounter(String key);

	/**
	 * Get counter for key,and if the key's value is not set,then set it with
	 * initial value.
	 * 
	 * @param key
	 * @param initialValue
	 * @return
	 */
	public Counter getCounter(String key, long initialValue);

	/**
	 * Get key iterator for special memcached server.You must known that the
	 * iterator is a snapshot for memcached all keys,it is not real-time.The
	 * 'stats cachedump" has length limitation,so iterator could not visit all
	 * keys if you have many keys.Your application should not be dependent on
	 * this feature.
	 * 
	 * @deprecated memcached 1.6.x will remove cachedump stats command,so this
	 *             method will be removed in the future
	 * @param address
	 * @return
	 */
	@Deprecated
	public KeyIterator getKeyIterator(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Configure auth info
	 * 
	 * @param map
	 *            Auth info map,key is memcached server address,and value is the
	 *            auth info for the key.
	 */
	public void setAuthInfoMap(Map<InetSocketAddress, AuthInfo> map);

	/**
	 * return current all auth info
	 * 
	 * @return Auth info map,key is memcached server address,and value is the
	 *         auth info for the key.
	 */
	public Map<InetSocketAddress, AuthInfo> getAuthInfoMap();

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 * @param delta
	 * @param initValue
	 *            the initial value to be added when value is not found
	 * @param timeout
	 * @param exp
	 *            the initial vlaue expire time, in seconds. Can be up to 30
	 *            days. After 30 days, is treated as a unix timestamp of an
	 *            exact date.
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	long decr(String key, long delta, long initValue, long timeout, int exp)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 *            key
	 * @param delta
	 *            increment delta
	 * @param initValue
	 *            the initial value to be added when value is not found
	 * @param timeout
	 *            operation timeout
	 * @param exp
	 *            the initial vlaue expire time, in seconds. Can be up to 30
	 *            days. After 30 days, is treated as a unix timestamp of an
	 *            exact date.
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	long incr(String key, long delta, long initValue, long timeout, int exp)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Return the cache instance name
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Set cache instance name
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Returns reconnecting task queue,the queue is thread-safe and 'weakly
	 * consistent',but maybe you <strong>should not modify it</strong> at all.
	 * 
	 * @return The reconnecting task queue,if the client has not been
	 *         started,returns null.
	 */
	public Queue<ReconnectRequest> getReconnectRequestQueue();

	/**
	 * Configure wheather to set client in failure mode.If set it to true,that
	 * means you want to configure client in failure mode. Failure mode is that
	 * when a memcached server is down,it would not taken from the server list
	 * but marked as unavailable,and then further requests to this server will
	 * be transformed to standby node if configured or throw an exception until
	 * it comes back up.
	 * 
	 * @param failureMode
	 *            true is to configure client in failure mode.
	 */
	public void setFailureMode(boolean failureMode);

	/**
	 * Returns if client is in failure mode.
	 * 
	 * @return
	 */
	public boolean isFailureMode();

	/**
	 * Set a key provider for pre-processing keys before sending them to
	 * memcached.
	 * 
	 * @since 1.3.8
	 * @param keyProvider
	 */
	public void setKeyProvider(KeyProvider keyProvider);

	/**
	 * Returns maximum number of timeout exception for closing connection.
	 * 
	 * @return
	 */
	public int getTimeoutExceptionThreshold();

	/**
	 * Set maximum number of timeout exception for closing connection.You can
	 * set it to be a large value to disable this feature.
	 * 
	 * @see #DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD
	 * @param timeoutExceptionThreshold
	 */
	public void setTimeoutExceptionThreshold(int timeoutExceptionThreshold);

	/**
	 * Invalidate all namespace under the namespace using the default operation
	 * timeout.
	 * 
	 * @since 1.4.2
	 * @param ns
	 *            the namespace
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public abstract void invalidateNamespace(String ns)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Invalidate all items under the namespace.
	 * 
	 * @since 1.4.2
	 * @param ns
	 *            the namespace
	 * @param opTimeout
	 *            operation timeout in milliseconds.
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public void invalidateNamespace(String ns, long opTimeout)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Remove current namespace set for this memcached client.It must begin with
	 * {@link #beginWithNamespace(String)} method.
	 * @see #beginWithNamespace(String)
	 */
	public void endWithNamespace();

	/**
	 * set current namespace for following operations with memcached client.It
	 * must be ended with {@link #endWithNamespace()} method.For example:
	 * <pre>
	 *    memcachedClient.beginWithNamespace(userId);
	 *    try{
	 *      memcachedClient.set("username",0,username);
	 *      memcachedClient.set("email",0,email);
	 *    }finally{
	 *      memcachedClient.endWithNamespace();
	 *    }
	 * </pre>
	 * @see #endWithNamespace()
	 * @see #withNamespace(String, MemcachedClientCallable)
	 * @param ns
	 */
	public void beginWithNamespace(String ns);

	/**
	 * With the namespae to do something with current memcached client.All
	 * operations with memcached client done in callable will be under the
	 * namespace. {@link #beginWithNamespace(String)} and {@link #endWithNamespace()} will called around automatically.
	 * For example:
	 * <pre>
	 *   memcachedClient.withNamespace(userId,new MemcachedClientCallable<Void>{
	 *     public Void call(MemcachedClient client) throws MemcachedException,
			InterruptedException, TimeoutException{
	 *      client.set("username",0,username);
	 *      client.set("email",0,email);
	 *      return null;
	 *     }
	 *   }); 
	 *   //invalidate all items under the namespace.
	 *   memcachedClient.invalidateNamespace(userId);
	 * </pre>
	 * 
	 * @since 1.4.2
	 * @param ns
	 * @param callable
	 * @see #beginWithNamespace(String)
	 * @see #endWithNamespace()
	 * @return
	 */
	public <T> T withNamespace(String ns, MemcachedClientCallable<T> callable)
			throws MemcachedException, InterruptedException, TimeoutException;

}