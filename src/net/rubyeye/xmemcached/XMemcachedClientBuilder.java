package net.rubyeye.xmemcached;

import com.google.code.yanf4j.config.Configuration;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.codec.text.MemcachedTextCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Builder pattern.Configure XmemcachedClient's options,then build it
 *
 * @author dennis
 *
 */
public class XMemcachedClientBuilder implements MemcachedClientBuilder {

	private MemcachedSessionLocator sessionLocator = new ArrayMemcachedSessionLocator();
	private BufferAllocator bufferAllocator = new SimpleBufferAllocator();
	private Configuration configuration = XMemcachedClient
			.getDefaultConfiguration();
	private List<InetSocketAddress> addressList;

	private @SuppressWarnings("unchecked")
	Transcoder transcoder = new SerializingTranscoder();

	private MemcachedCodecFactory<Command> codecFactory = new MemcachedTextCodecFactory(
			this.transcoder);

	/**
	 * return the memcached protocol codec factory,default is
	 * MemcachedTextCodecFactory
	 *
	 * @return
	 */
	public MemcachedCodecFactory<Command> getCodecFactory() {
		return codecFactory;
	}

	/**
	 * Set the memcached protocol codec factory,Default is
	 * MemcachedTextCodecFactory
	 *
	 * @param codecFactory
	 */
	public void setCodecFactory(MemcachedCodecFactory<Command> codecFactory) {
		this.codecFactory = codecFactory;
		this.codecFactory.setTranscoder(this.transcoder);
	}

	public XMemcachedClientBuilder(List<InetSocketAddress> addressList) {
		this.addressList = addressList;
	}

	public XMemcachedClientBuilder() {
		this(null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.rubyeye.xmemcached.MemcachedClientBuilder#getSessionLocator()
	 */
	public MemcachedSessionLocator getSessionLocator() {
		return sessionLocator;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClientBuilder#setSessionLocator(net.rubyeye
	 * .xmemcached.MemcachedSessionLocator)
	 */
	public void setSessionLocator(MemcachedSessionLocator sessionLocator) {
		this.sessionLocator = sessionLocator;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.rubyeye.xmemcached.MemcachedClientBuilder#getBufferAllocator()
	 */
	public BufferAllocator getBufferAllocator() {
		return bufferAllocator;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClientBuilder#setBufferAllocator(net.
	 * rubyeye.xmemcached.buffer.BufferAllocator)
	 */
	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.rubyeye.xmemcached.MemcachedClientBuilder#getConfiguration()
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.rubyeye.xmemcached.MemcachedClientBuilder#setConfiguration(com.google
	 * .code.yanf4j.config.Configuration)
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.rubyeye.xmemcached.MemcachedClientBuilder#build()
	 */
	public MemcachedClient build() throws IOException {
		return new XMemcachedClient(this.sessionLocator, this.bufferAllocator,
				this.configuration, this.codecFactory, this.transcoder,
				this.addressList);
	}

	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder() {
		return transcoder;
	}

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
		this.codecFactory.setTranscoder(transcoder);
	}

}
