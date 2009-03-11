package net.rubyeye.xmemcached.buffer;

import java.nio.ByteBuffer;

public class SimpleByteBufferWrapper implements ByteBufferWrapper {

	protected ByteBuffer origBuffer;

	public SimpleByteBufferWrapper(ByteBuffer origBuffer) {
		this.origBuffer = origBuffer;
	}

	@Override
	public void free() {
		// do nothing
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return this.origBuffer;
	}

	@Override
	public void put(byte[] bytes) {
		this.origBuffer.put(bytes);
	}

	@Override
	public int capacity() {
		return this.origBuffer.capacity();
	}

	@Override
	public void clear() {
		this.origBuffer.clear();
	}

	@Override
	public void flip() {
		this.origBuffer.flip();
	}

	@Override
	public int limit() {
		return this.origBuffer.limit();
	}

	@Override
	public void limit(int limit) {
		this.origBuffer.limit(limit);
	}

	@Override
	public int position() {
		return this.origBuffer.position();
	}

	@Override
	public void position(int pos) {
		this.origBuffer.position(pos);
	}

	@Override
	public void put(byte b) {
		this.origBuffer.put(b);
	}

}
