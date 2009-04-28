package net.rubyeye.xmemcached;

import com.google.code.yanf4j.config.Configuration;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;

/**
 * builder模式，用于创建XMemcachedClient
 * 
 * @author Administrator
 * 
 */
public class XMemcachedClientBuilder {

	private MemcachedSessionLocator sessionLocator = new ArrayMemcachedSessionLocator();
	private BufferAllocator bufferAllocator = new SimpleBufferAllocator();
	private Configuration configuration = XMemcachedClient
			.getDefaultConfiguration();
	private List<InetSocketAddress> addressList;

	public XMemcachedClientBuilder(List<InetSocketAddress> addressList) {
		this.addressList = addressList;
	}

	public XMemcachedClientBuilder() {
		this(null);
	}

	public MemcachedSessionLocator getSessionLocator() {
		return sessionLocator;
	}

	public void setSessionLocator(MemcachedSessionLocator sessionLocator) {
		this.sessionLocator = sessionLocator;
	}

	public BufferAllocator getBufferAllocator() {
		return bufferAllocator;
	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public XMemcachedClient build() throws IOException {
		return new XMemcachedClient(this.sessionLocator, this.bufferAllocator,
				this.configuration, this.addressList);
	}
}
