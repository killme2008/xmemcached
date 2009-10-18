package net.rubyeye.xmemcached.uds;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.SocketOption;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedClientStateListener;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class UDSMemcachedClientBuilder implements MemcachedClientBuilder {

	public void addStateListener(MemcachedClientStateListener stateListener) {
		// TODO Auto-generated method stub

	}

	public MemcachedClient build() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public BufferAllocator getBufferAllocator() {
		// TODO Auto-generated method stub
		return null;
	}

	public CommandFactory getCommandFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	public Configuration getConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	public MemcachedSessionLocator getSessionLocator() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<SocketOption, Object> getSocketOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	public Transcoder getTranscoder() {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeStateListener(MemcachedClientStateListener stateListener) {
		// TODO Auto-generated method stub

	}

	public void setBufferAllocator(BufferAllocator bufferAllocator) {
		// TODO Auto-generated method stub

	}

	public void setCommandFactory(CommandFactory commandFactory) {
		// TODO Auto-generated method stub

	}

	public void setConfiguration(Configuration configuration) {
		// TODO Auto-generated method stub

	}

	public void setConnectionPoolSize(int poolSize) {
		// TODO Auto-generated method stub

	}

	public void setSessionLocator(MemcachedSessionLocator sessionLocator) {
		// TODO Auto-generated method stub

	}

	public void setSocketOption(SocketOption socketOption, Object value) {
		// TODO Auto-generated method stub

	}

	public void setStateListeners(
			List<MemcachedClientStateListener> stateListeners) {
		// TODO Auto-generated method stub

	}

	public void setTranscoder(Transcoder transcoder) {
		// TODO Auto-generated method stub

	}

}
