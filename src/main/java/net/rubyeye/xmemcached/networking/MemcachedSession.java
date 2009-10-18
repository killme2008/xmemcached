package net.rubyeye.xmemcached.networking;

import net.rubyeye.xmemcached.buffer.BufferAllocator;

public interface MemcachedSession {
	public int getWeight();
	public void setAllowReconnect(boolean allow);
	public boolean isAllowReconnect();
	public void setBufferAllocator(BufferAllocator allocator);
}
