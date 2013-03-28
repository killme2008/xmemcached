package net.rubyeye.xmemcached.test.unittest.buffer;

import java.nio.BufferOverflowException;
import java.nio.ByteOrder;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import junit.framework.TestCase;

public abstract class AbstractBufferAllocatorUnitTest extends TestCase {

	protected BufferAllocator allocator;

	@Override
	protected void setUp() throws Exception {
		createBufferAllocator();
	}

	public abstract void createBufferAllocator();

	public void testEmptyBuffer() {
		IoBuffer emptyBuffer = this.allocator.allocate(0);

		assertNotNull(emptyBuffer);
		assertEquals(0, emptyBuffer.capacity());
		assertEquals(0, emptyBuffer.position());
		assertEquals(0, emptyBuffer.limit());

		try {
			emptyBuffer.put((byte) 0);
			fail();
		} catch (BufferOverflowException e) {
			assertTrue(true);
		}

		assertSame(emptyBuffer, this.allocator.allocate(0));
	}

	public void testAllocate() {
		IoBuffer buffer = this.allocator.allocate(64);
		assertNotNull(buffer);
		assertEquals(ByteOrder.BIG_ENDIAN, buffer.order());
		assertNotNull(buffer.getByteBuffer());

		assertEquals(64, buffer.capacity());
		assertEquals(0, buffer.position());
		assertEquals(64, buffer.limit());

		buffer.put("test".getBytes());
		assertEquals(64, buffer.capacity());
		assertEquals(4, buffer.position());
		assertEquals(64, buffer.limit());
		assertEquals(60, buffer.remaining());
		buffer.mark();

		buffer.position(32);
		assertEquals(32, buffer.position());
		buffer.reset();
		assertEquals(4, buffer.position());

		assertTrue(buffer.hasRemaining());
		buffer.position(64);
		assertFalse(buffer.hasRemaining());

		buffer.order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(ByteOrder.LITTLE_ENDIAN, buffer.order());
		buffer.order(ByteOrder.BIG_ENDIAN);

		buffer.position(4);
		buffer.flip();
		assertEquals(0, buffer.position());
		assertEquals(4, buffer.limit());
		assertEquals(4, buffer.remaining());
		
		buffer.clear();
		assertEquals(64, buffer.capacity());
		assertEquals(0, buffer.position());
		assertEquals(64, buffer.limit());

		buffer.free();
		assertNull(buffer.getByteBuffer());
	}

	@Override
	protected void tearDown() throws Exception {
		this.allocator.dispose();
	}

}
