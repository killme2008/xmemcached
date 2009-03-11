package net.rubyeye.xmemcached.buffer;

import java.nio.ByteBuffer;

/**
 * 简单ByteBuffer分配器
 * 
 * @author dennis
 * 
 */
public class SimpleBufferAllocator implements BufferAllocator {

	public ByteBufferWrapper allocate(int capacity) {
		return new SimpleByteBufferWrapper(ByteBuffer.allocate(capacity));
	}

	public void dispose() {
	}

}
