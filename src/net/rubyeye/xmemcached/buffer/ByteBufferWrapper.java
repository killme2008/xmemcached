package net.rubyeye.xmemcached.buffer;

import java.nio.ByteBuffer;

public interface ByteBufferWrapper {
	public ByteBuffer getByteBuffer();

	public void put(byte[] bytes);

	public void free();

	public void clear();

	public int position();

	public int capacity();

	public void position(int pos);

	public void limit(int limit);

	public int limit();
	
	public void flip();
	
	public void put(byte b);
}
