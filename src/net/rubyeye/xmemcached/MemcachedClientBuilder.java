package net.rubyeye.xmemcached;

import java.io.IOException;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.transcoders.Transcoder;

import com.google.code.yanf4j.config.Configuration;

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
	public abstract MemcachedSessionLocator getSessionLocator();

	/**
	 * Set the XmemcachedClient's session locator.Use
	 * ArrayMemcachedSessionLocator by default.If you want to choose consistent
	 * hash strategy,set it to KetamaMemcachedSessionLocator
	 *
	 * @param sessionLocator
	 */
	public abstract void setSessionLocator(
			MemcachedSessionLocator sessionLocator);

	public abstract BufferAllocator getBufferAllocator();

	/**
	 * Set nio ByteBuffer's allocator.Use SimpleBufferAllocator by default.You
	 * can choose CachedBufferAllocator.
	 *
	 * @param bufferAllocator
	 */
	public abstract void setBufferAllocator(BufferAllocator bufferAllocator);

	/**
	 * Return the default networking's configuration,you can change them.
	 *
	 * @return
	 */
	public abstract Configuration getConfiguration();

	/**
	 * Set the XmemcachedClient's networking
	 * configuration(reuseAddr,receiveBufferSize,tcpDelay etc.)
	 *
	 * @param configuration
	 */
	public abstract void setConfiguration(Configuration configuration);

	/**
	 * Build MemcachedClient by current options.
	 *
	 * @return
	 * @throws IOException
	 */
	public abstract MemcachedClient build() throws IOException;

	/**
	 * return the memcached protocol codec factory,default is
	 * MemcachedTextCodecFactory
	 *
	 * @return
	 */
	public MemcachedCodecFactory<Command> getCodecFactory();

	/**
	 * Set the memcached protocol codec factory,Default is
	 * MemcachedTextCodecFactory
	 *
	 * @param codecFactory
	 */
	public void setCodecFactory(MemcachedCodecFactory<Command> codecFactory);

	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder();

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder);

}