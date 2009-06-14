package net.rubyeye.xmemcached;

import com.google.code.yanf4j.config.Configuration;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
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

	private List<ReconnectRequest> connectRequestList;

	private CommandFactory commandFactory = new TextCommandFactory();

	public final CommandFactory getCommandFactory() {
		return commandFactory;
	}

	public final void setCommandFactory(CommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}

	private @SuppressWarnings("unchecked")
	Transcoder transcoder = new SerializingTranscoder();

	public XMemcachedClientBuilder(List<InetSocketAddress> addressList) {
		this.addressList = addressList;
	}

	/**
	 * Create a XMemcachedClientBuilder,enable weighted server.
	 * 
	 * @param connectRequestList
	 * @return
	 */
	public static XMemcachedClientBuilder newMemcachedClientBuilder(
			List<ReconnectRequest> connectRequestList) {
		XMemcachedClientBuilder builder = new XMemcachedClientBuilder();
		builder.connectRequestList = connectRequestList;
		return builder;
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
		if (this.connectRequestList == null)
			return new XMemcachedClient(this.sessionLocator,
					this.bufferAllocator, this.configuration,
					this.commandFactory, this.transcoder, this.addressList);
		else {
			List<InetSocketAddress> list = new ArrayList<InetSocketAddress>(
					this.connectRequestList.size());
			int[] weights = new int[this.connectRequestList.size()];
			for (int i = 0; i < weights.length; i++) {
				ReconnectRequest reconnectRequest = this.connectRequestList
						.get(i);
				list.add(reconnectRequest.getAddress());
				weights[i] = reconnectRequest.getWeight();
			}
			return new XMemcachedClient(this.sessionLocator,
					this.bufferAllocator, this.configuration,
					this.commandFactory, this.transcoder,list,
					weights);
		}
	}

	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder() {
		return transcoder;
	}

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

}
