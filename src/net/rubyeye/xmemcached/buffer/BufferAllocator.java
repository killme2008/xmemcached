package net.rubyeye.xmemcached.buffer;

public interface BufferAllocator {
	public ByteBufferWrapper allocate(int capacity);
	
	public void dispose();
}
