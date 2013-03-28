package net.rubyeye.xmemcached.test.unittest.buffer;


import net.rubyeye.xmemcached.buffer.CachedBufferAllocator;

public class CachedBufferAllocatorUnitTest extends AbstractBufferAllocatorUnitTest {
	public void createBufferAllocator() {
		this.allocator = CachedBufferAllocator.newInstance();
	}

}
