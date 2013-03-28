package net.rubyeye.xmemcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.SocketOption;

/**
 * Builder pattern.Configure XmemcachedClient's options,then build it
 * 
 * @author dennis
 * 
 */
public interface MemcachedClientBuilder {

	/**
	 * 
	 * @return net.rubyeye.xmemcached.MemcachedSessionLocator
	 */
	public MemcachedSessionLocator getSessionLocator();

	/**
	 * Set the XmemcachedClient's session locator.Use
	 * ArrayMemcachedSessionLocator by default.If you want to choose consistent
	 * hash strategy,set it to KetamaMemcachedSessionLocator
	 * 
	 * @param sessionLocator
	 */
	public void setSessionLocator(MemcachedSessionLocator sessionLocator);

	public BufferAllocator getBufferAllocator();

	/**
	 * Set nio ByteBuffer's allocator.Use SimpleBufferAllocator by default.You
	 * can choose CachedBufferAllocator.
	 * 
	 * @param bufferAllocator
	 */
	public void setBufferAllocator(BufferAllocator bufferAllocator);

	/**
	 * Return the default networking's configuration,you can change them.
	 * 
	 * @return
	 */
	public Configuration getConfiguration();

	/**
	 * Set the XmemcachedClient's networking
	 * configuration(reuseAddr,receiveBufferSize,tcpDelay etc.)
	 * 
	 * @param configuration
	 */
	public void setConfiguration(Configuration configuration);

	/**
	 * Build MemcachedClient by current options.
	 * 
	 * @return
	 * @throws IOException
	 */
	public MemcachedClient build() throws IOException;

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
	 *            pool size,default is 1
	 */
	public void setConnectionPoolSize(int poolSize);

	/**
	 * Set xmemcached's transcoder,it is used for seriailizing
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder();

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder);

	/**
	 * get xmemcached's command factory
	 * 
	 * @return
	 */
	public CommandFactory getCommandFactory();

	/**
	 * Add a state listener
	 * 
	 * @param stateListener
	 */
	public void addStateListener(MemcachedClientStateListener stateListener);

	/**
	 * Remove a state listener
	 * 
	 * @param stateListener
	 */
	public void removeStateListener(MemcachedClientStateListener stateListener);

	/**
	 * Set state listeners,replace current list
	 * 
	 * @param stateListeners
	 */
	public void setStateListeners(
			List<MemcachedClientStateListener> stateListeners);

	/**
	 * set xmemcached's command factory.Default is TextCommandFactory,which
	 * implements memcached text protocol.
	 * 
	 * @param commandFactory
	 */
	public void setCommandFactory(CommandFactory commandFactory);

	/**
	 * Set tcp socket option
	 * 
	 * @param socketOption
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public void setSocketOption(SocketOption socketOption, Object value);

	/**
	 * Get all tcp socket options
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<SocketOption, Object> getSocketOptions();

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
	 * Add auth info for memcached server
	 * 
	 * @param address
	 * @param authInfo
	 */
	public void addAuthInfo(InetSocketAddress address, AuthInfo authInfo);

	/**
	 * Remove auth info for memcached server
	 * 
	 * @param address
	 */
	public void removeAuthInfo(InetSocketAddress address);

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
	 * Returns connect timeout in milliseconds
	 * 
	 * @return connect timeout
	 */
	public long getConnectTimeout();

	/**
	 * Set connect timeout in milliseconds
	 * 
	 * @see net.rubyeye.xmemcached.MemcachedClient#DEFAULT_CONNECT_TIMEOUT
	 * 
	 * @param connectTimeout
	 */
	public void setConnectTimeout(long connectTimeout);

	/**
	 * Enables/disables sanitizing keys by URLEncoding.
	 * 
	 * @param sanitizeKey
	 *            if true, then URLEncode all keys
	 */
	public void setSanitizeKeys(boolean sanitizeKeys);

	/**
	 * Set a key provider for pre-processing keys before sending them to
	 * memcached.
	 * 
	 * @since 1.3.8
	 * @param keyProvider
	 */
	public void setKeyProvider(KeyProvider keyProvider);

	/**
	 * Set max queued noreply operations number
	 * 
	 * @see MemcachedClient#DEFAULT_MAX_QUEUED_NOPS
	 * @param maxQueuedNoReplyOperations
	 * @since 1.3.8
	 */
	public void setMaxQueuedNoReplyOperations(int maxQueuedNoReplyOperations);
	
	/**
	 * If the memcached dump or network error cause connection closed,xmemcached
	 * would try to heal the connection.The interval between reconnections is 2
	 * seconds by default. You can change that value by this method.
	 * 
	 * @since 1.3.9
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
	 * Set default operation timeout.
	 * @param opTimeout  Operation timeout value in milliseconds.
	 * @since 1.4.1
	 */
	public void setOpTimeout(long opTimeout);

	/**
	 * Returns the default operation timeout in milliseconds.
	 * @since 1.4.1
	 * @return
	 */
	public long getOpTimeout();

}