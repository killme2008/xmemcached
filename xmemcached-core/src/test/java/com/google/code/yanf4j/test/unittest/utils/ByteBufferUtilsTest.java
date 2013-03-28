package com.google.code.yanf4j.test.unittest.utils;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.util.ByteBufferUtils;

public class ByteBufferUtilsTest extends TestCase {

	public void testIncreaseBlankBufferCapatity() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer = ByteBufferUtils.increaseBufferCapatity(buffer);

		assertEquals(1024 + Configuration.DEFAULT_INCREASE_BUFF_SIZE, buffer
				.capacity());
		buffer = ByteBufferUtils.increaseBufferCapatity(buffer);
		assertEquals(1024 + 2 * Configuration.DEFAULT_INCREASE_BUFF_SIZE,
				buffer.capacity());

	}

	public void testIncreaseNotBlankBufferCapatity() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.putInt(100);
		buffer = ByteBufferUtils.increaseBufferCapatity(buffer);
		assertEquals(1024 + Configuration.DEFAULT_INCREASE_BUFF_SIZE, buffer
				.capacity());
		assertEquals(4, buffer.position());
		assertEquals(1024 + Configuration.DEFAULT_INCREASE_BUFF_SIZE - 4,
				buffer.remaining());
		buffer.putLong(100l);
		assertEquals(12, buffer.position());
		buffer = ByteBufferUtils.increaseBufferCapatity(buffer);
		assertEquals(12, buffer.position());
		assertEquals(1024 + 2 * Configuration.DEFAULT_INCREASE_BUFF_SIZE - 4
				- 8, buffer.remaining());

	}

	public void testIncreaseNullBufferCapacity() {
		try {
			assertNull(ByteBufferUtils.increaseBufferCapatity(null));
		} catch (IllegalArgumentException e) {
			assertEquals("buffer is null", e.getMessage());
		}
	}

	public void testFlip() {
		ByteBuffer[] buffers = new ByteBuffer[2];
		ByteBufferUtils.flip(buffers);
		buffers[0] = ByteBuffer.allocate(4).putInt(4);
		buffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());

		assertEquals(4, buffers[0].position());
		assertEquals(5, buffers[1].position());
		assertEquals(4, buffers[0].limit());
		assertEquals(10, buffers[1].limit());
		ByteBufferUtils.flip(buffers);
		assertEquals(0, buffers[0].position());
		assertEquals(0, buffers[1].position());
		assertEquals(4, buffers[0].limit());
		assertEquals(5, buffers[1].limit());

		ByteBufferUtils.flip(null);
	}

	public void testClear() {
		ByteBuffer[] buffers = new ByteBuffer[2];
		ByteBufferUtils.clear(buffers);
		buffers[0] = ByteBuffer.allocate(4).putInt(4);
		buffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());

		assertEquals(4, buffers[0].position());
		assertEquals(5, buffers[1].position());
		assertEquals(4, buffers[0].limit());
		assertEquals(10, buffers[1].limit());
		assertEquals(0, buffers[0].remaining());
		assertEquals(5, buffers[1].remaining());
		ByteBufferUtils.clear(buffers);
		assertEquals(0, buffers[0].position());
		assertEquals(0, buffers[1].position());
		assertEquals(4, buffers[0].limit());
		assertEquals(10, buffers[1].limit());
		assertEquals(4, buffers[0].remaining());
		assertEquals(10, buffers[1].remaining());
		ByteBufferUtils.clear(null);
	}

	public void testHasRemaining() {
		ByteBuffer[] buffers = new ByteBuffer[2];
		assertFalse(ByteBufferUtils.hasRemaining(buffers));
		buffers[0] = ByteBuffer.allocate(4).putInt(4);
		buffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());
		assertTrue(ByteBufferUtils.hasRemaining(buffers));

		buffers[1].put("yanfj".getBytes());
		assertFalse(ByteBufferUtils.hasRemaining(buffers));

		ByteBufferUtils.clear(buffers);
		assertTrue(ByteBufferUtils.hasRemaining(buffers));
		ByteBuffer[] moreBuffers = new ByteBuffer[3];
		moreBuffers[0] = ByteBuffer.allocate(4).putInt(4);
		moreBuffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());
		moreBuffers[2] = ByteBuffer.allocate(12).putLong(9999);
		assertTrue(ByteBufferUtils.hasRemaining(moreBuffers));
		moreBuffers[2].putInt(4);
		assertTrue(ByteBufferUtils.hasRemaining(moreBuffers));
		moreBuffers[1].put("yanfj".getBytes());
		assertFalse(ByteBufferUtils.hasRemaining(moreBuffers));

		assertFalse(ByteBufferUtils.hasRemaining(null));
	}

	public void testIndexOf() {
		String words = "hello world good hello";
		ByteBuffer buffer = ByteBuffer.wrap(words.getBytes());

		String world = "world";
		assertEquals(6, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap(world
				.getBytes())));
		assertEquals(0, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap("hello"
				.getBytes())));
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			assertEquals(17, ByteBufferUtils.indexOf(buffer, ByteBuffer
					.wrap("hello".getBytes()), 6));
		}
		System.out.println(System.currentTimeMillis() - start);
		assertEquals(-1, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap("test"
				.getBytes())));
		assertEquals(-1, ByteBufferUtils.indexOf(buffer, (ByteBuffer) null));
		assertEquals(-1, ByteBufferUtils.indexOf(null, buffer));
	}

	public void testGather() {
		ByteBuffer buffer1 = ByteBuffer.wrap("hello".getBytes());
		ByteBuffer buffer2 = ByteBuffer.wrap(" dennis".getBytes());

		ByteBuffer gather=ByteBufferUtils.gather(new ByteBuffer[] { buffer1, buffer2 });
		
		assertEquals("hello dennis",new String(gather.array()));
		
		assertNull(ByteBufferUtils.gather(null));
		assertNull(ByteBufferUtils.gather(new ByteBuffer[]{}));
	}

}
