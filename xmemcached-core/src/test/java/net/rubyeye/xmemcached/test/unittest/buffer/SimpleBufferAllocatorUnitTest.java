package net.rubyeye.xmemcached.test.unittest.buffer;

import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;

public class SimpleBufferAllocatorUnitTest extends
		AbstractBufferAllocatorUnitTest {

	@Override
	public void createBufferAllocator() {
		this.allocator = SimpleBufferAllocator.newInstance();

	}

}
