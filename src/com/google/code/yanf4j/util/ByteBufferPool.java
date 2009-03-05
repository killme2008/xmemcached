package com.google.code.yanf4j.util;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ByteBuffer分配池
 * 
 * @author dennis
 * 
 */
public class ByteBufferPool {
	private int capacity;

	private AtomicInteger size;

	public static final int[] BUFFER_LEVELS = { 16, 32, 64, 128, 256, 512,
			1024, 2 * 1024, 4 * 1024, 8 * 1024, 16 * 1024, 32 * 1024,
			64 * 1024};

	private List<ByteBuffer>[] buffers;

	private Lock[] locks;

	public ByteBufferPool() {
		this(1024 * 1024);
	}

	public ByteBufferPool(int capacity) {
		if (capacity <= 0)
			throw new IllegalArgumentException();
		this.capacity = capacity;
		this.buffers = new List[BUFFER_LEVELS.length];
		this.locks = new Lock[BUFFER_LEVELS.length];
		for (int i = 0; i < BUFFER_LEVELS.length; i++) {
			buffers[i] = new LinkedList<ByteBuffer>();
			locks[i] = new ReentrantLock();
		}
		this.size = new AtomicInteger(0);
	}

	public ByteBuffer allocate(int maxLength) {
		if (maxLength < 0)
			throw new IllegalArgumentException();
		if (maxLength <= BUFFER_LEVELS[0]
				|| maxLength >= BUFFER_LEVELS[BUFFER_LEVELS.length - 1]) {
			return ByteBuffer.allocate(maxLength);
		}
		int targetIndex = getTargetIndex(maxLength);
		if (targetIndex < 0)
			return ByteBuffer.allocate(maxLength);
		List<ByteBuffer> targetList = buffers[targetIndex+1];
		locks[targetIndex].lock();
		try {
			if (targetList.isEmpty()) {
				ByteBuffer byteBuffer = ByteBuffer
						.allocate(BUFFER_LEVELS[targetIndex + 1]);
				byteBuffer.limit(maxLength);
				return byteBuffer;
			} else {
				ByteBuffer byteBuffer = targetList
						.remove(targetList.size() - 1);
				this.size.addAndGet(0 - byteBuffer.capacity());
				byteBuffer.limit(maxLength);
				return byteBuffer;
			}
		} finally {
			locks[targetIndex].unlock();
		}
	}

	public void clear() {
		for (int i = 0; i < BUFFER_LEVELS.length; i++)
			buffers[i].clear();
		this.size.set(0);
	}

	public void free(ByteBuffer byteBuffer) {
		if (byteBuffer == null)
			return;
		if (byteBuffer.capacity() <= BUFFER_LEVELS[0]
				|| byteBuffer.capacity() >= BUFFER_LEVELS[BUFFER_LEVELS.length - 1]) {
			return;
		}
		byteBuffer.clear();
		if (this.size.get() >= this.capacity)
			return;
		int targetIndex = getTargetIndex(byteBuffer.capacity());
		if (targetIndex < 0)
			return;
		List<ByteBuffer> targetList = buffers[targetIndex+1];
		locks[targetIndex].lock();
		try {
			targetList.add(byteBuffer);
			this.size.addAndGet(byteBuffer.capacity());
		} finally {
			locks[targetIndex].unlock();
		}
	}

	public void flip(ByteBuffer byteBuffer) {
		byteBuffer.position(0);
	}

	public static int getTargetIndex(int maxLength) {
		double e = Math.log(maxLength) / e2;

		int pos = (int) (e - fPos);
		if (e - fPos - pos == 0.0000) {
			pos = pos - 1;
		}
		return pos < -1 ? -1 : pos;
	}

	static final double e2 = Math.log(2);

	static final double fPos = Math.log(BUFFER_LEVELS[0]) / e2;

	public int size() {
		return this.size.get();
	}
}
