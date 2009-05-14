package net.rubyeye.xmemcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.MemcachedConnector;
import net.rubyeye.xmemcached.transcoders.Transcoder;

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
	 * server's number on linux platform,keep default on windows.
	 */
	public static final int DEFAULT_READ_THREAD_COUNT = 0;
	/**
	 * Default connect timeout,1 minutes
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60000;
	/**
	 * Default socket's send buffer size,16k
	 */
	public static final int DEFAULT_TCP_SEND_BUFF_SIZE = 16 * 1024;
	/**
	 * Enable Nagle algorithm by default
	 */
	public static final boolean DEFAULT_TCP_NO_DELAY = false;
	/**
	 * Default session read buffer size,32K
	 */
	public static final int DEFAULT_SESSION_READ_BUFF_SIZE = 32 * 1024;
	/**
	 * Default socket's receive buffer size,16k
	 */
	public static final int DEFAULT_TCP_RECV_BUFF_SIZE = 16 * 1024;
	/**
	 * Default operation timeout,if the operation is not returned in 1
	 * second,throw TimeoutException
	 */
	public static final long DEFAULT_OP_TIMEOUT = 1000;

	/**
	 * Set the merge factor,this factor determins how many 'get' commands would
	 * be merge to one multi-get command.default is 150
	 *
	 * @param mergeFactor
	 */
	public abstract void setMergeFactor(final int mergeFactor);

	/**
	 * Get the connect timeout
	 *
	 * @param connectTimeout
	 */
	public abstract long getConnectTimeout();

	/**
	 * Set the connect timeout,default is 1 minutes
	 *
	 * @param connectTimeout
	 */
	public abstract void setConnectTimeout(long connectTimeout);

	/**
	 * return the session manager
	 *
	 * @return
	 */
	public abstract MemcachedConnector getConnector();

	/**
	 * Enable/Disable merge many get commands to one multi-get command.true is
	 * to enable it,false is to disable it.Default is true.Recommend users to
	 * enable it.
	 *
	 * @param optimiezeGet
	 */
	public abstract void setOptimiezeGet(final boolean optimiezeGet);

	/**
	 * Enable/Disable merge many command's buffers to one big buffer fit
	 * socket's send buffer size.Default is true.Recommend true.
	 *
	 * @param optimizeMergeBuffer
	 */
	public abstract void setOptimizeMergeBuffer(
			final boolean optimizeMergeBuffer);

	/**
	 * @return
	 */
	public abstract boolean isShutdown();

	/**
	 * Aadd a memcached server,the thread call this method will be blocked until
	 * the connecting operations completed(success or fail)
	 *
	 * @param server
	 *            host string
	 * @param port
	 *            port number
	 */
	public abstract void addServer(final String server, final int port)
			throws IOException;

	/**
	 * Add a memcached server,the thread call this method will be blocked until
	 * the connecting operations completed(success or fail)
	 *
	 * @param inetSocketAddress
	 *            memcached server's socket address
	 */
	public abstract void addServer(final InetSocketAddress inetSocketAddress)
			throws IOException;

	/**
	 * Add many memcached servers.You can call this method through JMX or
	 * program
	 *
	 * @param host
	 *            String like [host1]:[port1] [host2]:[port2] ...
	 */
	public abstract void addServer(String hostList) throws IOException;

	/**
	 * Get current server list.You can call this method through JMX or program
	 */
	public abstract List<String> getServersDescription();

	/**
	 * Remove many memcached server
	 *
	 * @param host
	 *            String like [host1]:[port1] [host2]:[port2] ...
	 */
	public abstract void removeServer(String hostList);

	/**
	 * Set the nio's ByteBuffer Allocator,use SimpleBufferAllocator by default.
	 *
	 * @param bufferAllocator
	 * @return
	 */
	public abstract void setBufferAllocator(
			final BufferAllocator bufferAllocator);

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
	public abstract <T> T get(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract Object get(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> T get(final String key, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract Object get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 类似get,但是gets将返回缓存项的cas值，可用于cas操作，参见cas方法
	 *
	 * @param <T>
	 * @param key
	 *            关键字
	 * @param timeout
	 *            操作的超时时间
	 * @param transcoder
	 *            数据项的反序列化转换器
	 * @return GetsResponse 返回GetsResponse对象
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> GetsResponse<T> gets(final String key,
			final long timeout, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> GetsResponse<T> gets(final String key)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> GetsResponse<T> gets(final String key,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	@SuppressWarnings("unchecked")
	public abstract <T> GetsResponse<T> gets(final String key,
			final Transcoder transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * memcached的getMulti操作，批量获取一批key对应的数据项
	 *
	 * @param <T>
	 * @param keyCollections
	 *            关键字集合
	 * @param timeout
	 *            操作超时时间
	 * @param transcoder
	 *            数据项的反序列化转换器
	 * @return map对象，map中是缓存中存在着的数据项，如果不存在将不会在map中出现
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 类似getMulti，但是返回数据项的cas值，返回的map中value存储的是GetsResponse对象
	 *
	 * @param <T>
	 * @param keyCollections
	 * @param timeout
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 设置key对应的项为value，无论key是否已经存在
	 *
	 * @param <T>
	 * @param key
	 *            缓存关键字
	 * @param exp
	 *            缓存的超时时间
	 * @param value
	 *            缓存的值对象
	 * @param transcoder
	 *            对象的序列化转换器
	 * @param timeout
	 *            操作的超时时间，单位是毫秒
	 * @return 成功返回true，否则返回false
	 * @throws TimeoutException
	 *             操作超时抛出此异常
	 * @throws InterruptedException
	 *             操作被中断
	 * @throws MemcachedException
	 *             memcached异常，可能是客户端或者memcached server返回的错误
	 */
	public abstract <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean set(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean set(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 *添加key-value缓存项，仅在key不存在的情况下才能添加成功
	 *
	 * @param <T>
	 * @param key
	 * @param exp
	 *            缓存的超时时间，0为永不过期（memcached默认为一个月）
	 * @param value
	 *            缓存的值对象
	 * @param transcoder
	 *            值对象的转换器
	 * @param timeout
	 *            操作的超时时间，单位毫秒
	 * @return 成功返回true，否则返回false
	 * @throws TimeoutException
	 *             操作超时抛出此异常
	 * @throws InterruptedException
	 *             操作被中断
	 * @throws MemcachedException
	 *             memcached异常，可能是客户端或者memcached server返回的错误
	 */
	public abstract <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean add(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean add(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 替代key对应的值，当且仅当key对应的缓存项存在的时候可以替换
	 *
	 * @param <T>
	 * @param key
	 * @param exp
	 *            缓存的超时时间
	 * @param value
	 *            值对象
	 * @param transcoder
	 *            值对象的转换器
	 * @param timeout
	 *            操作的超时时间,单位毫秒
	 * @return 如果key不存在返回false，如果替代成功返回true
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean replace(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean replace(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 将value添加到key对应的缓存项后面连接起来，这一操作仅对String有意义。
	 *
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean append(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * 类似append，是将value附加到key对应的缓存项前面，这一操作仅对String有实际意义
	 *
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract boolean prepend(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean prepend(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean cas(final String key, final int exp,
			final Object value, final long cas) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * cas原子替换key对应的value，当且仅当cas值相等的时候替换成功
	 *
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout,
			final long cas) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean cas(final String key, final int exp,
			final Object value, final long timeout, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 原子替换key对应的value值，当且仅当cas值相等时替换成功，具体使用参见wiki
	 *
	 * @param <T>
	 * @param key
	 * @param exp
	 *            缓存数据项的超时时间
	 * @param operation
	 *            CASOperation对象，包装cas操作
	 * @param transcoder
	 *            对象转换器
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 原子替换key对应的value值，当且仅当cas值相等时替换成功
	 *
	 * @param <T>
	 * @param key
	 * @param exp
	 *            缓存数据项的超时时间
	 * @param getsReponse
	 *            gets返回的结果
	 * @param operation
	 *            CASOperation操作
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 从缓存中移除key对应的数据项,memcached移除数据项，如果指定了time，那么将放入一个delete
	 * queue，直到时间到达才真正移除，在此段时间内，add、replace同一个key的操作将失败
	 *
	 * @param key
	 * @param time
	 *            单位为秒，客户端希望memcached server拒绝接受相同key的add,replace操作的时间
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 获取memcached版本，此方法在多个节点的情况下将按照"version"字符串的hash值查找对应的连接并发送version协议，
	 * 也就说此方法仅返回某个节点的memcached版本，如果要查询特定节点的memcached版本，请参考stats方法
	 *
	 * @return 版本号字符串
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract String version() throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 递增key对应的value
	 *
	 * @param key
	 * @param num
	 *            增加的幅度
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract int incr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 递减key对应的value
	 *
	 * @param key
	 * @param num
	 *            递减的幅度
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract int decr(final String key, final int num)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * 使cache中所有的数据项失效，如果是连接多个节点的memcached，那么所有的memcached中的数据项都将失效
	 *
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll() throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 使cache中所有的数据项失效,如果是连接多个节点的memcached，那么所有的memcached中的数据项都将失效
	 *
	 * @param timeout
	 *            操作超时时间
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 使指定memcached节点的数据项失效
	 *
	 * @param host
	 *            memcached节点host ip:port的形式
	 * @param timeout
	 *            操作超时时间
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll(String host, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract void flushAll(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	public abstract void flushAll(InetSocketAddress address, long timeout)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * 使指定memcached节点的数据项失效
	 *
	 * @param host
	 *            memcached节点host ip:port的形式
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll(String host) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 查看指定节点的memcached server统计信息
	 *
	 * @param host
	 *            memcached节点host ip:port的形式
	 * @param timeout
	 *            操作超时
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract Map<String, String> stats(String host, long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract Map<String, String> stats(String host)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * 查看特定节点的memcached server统计信息
	 *
	 * @param address
	 *            节点地址
	 * @param timeout
	 *            操作超时
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public abstract Map<String, String> stats(InetSocketAddress address,
			long timeout) throws MemcachedException, InterruptedException,
			TimeoutException;

	public abstract void shutdown() throws IOException;

	public abstract boolean delete(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 返回默认的序列化转换器，默认使用SerializingTranscoder
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public abstract Transcoder getTranscoder();

	/**
	 * 设置默认的序列化转换器，在调用xmemcached各种方法时，如果没有指定转换器，将使用此默认转换器
	 *
	 * @param transcoder
	 */
	@SuppressWarnings("unchecked")
	public abstract void setTranscoder(final Transcoder transcoder);

}