package net.rubyeye.xmemcached.buffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.CircularQueue;

/**
 * ThreadLocal范围内的ByteBuffer缓存分配器，来源于mina2.0
 * 
 * @author dennis
 * 
 */
public class CachedBufferAllocator implements BufferAllocator {

	private static final int DEFAULT_MAX_POOL_SIZE = 8;
	private static final int DEFAULT_MAX_CACHED_BUFFER_SIZE = 1 << 18; // 256KB

	private final int maxPoolSize;
	private final int maxCachedBufferSize;

	private final ThreadLocal<Map<Integer, Queue<CachedByteBufferWrapper>>> heapBuffers;

	/**
	 * Creates a new instance with the default parameters ({@literal #DEFAULT_MAX_POOL_SIZE}
	 * and {@literal #DEFAULT_MAX_CACHED_BUFFER_SIZE}).
	 */
	public CachedBufferAllocator() {
		this(DEFAULT_MAX_POOL_SIZE, DEFAULT_MAX_CACHED_BUFFER_SIZE);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param maxPoolSize
	 *            the maximum number of buffers with the same capacity per
	 *            thread. <tt>0</tt> disables this limitation.
	 * @param maxCachedBufferSize
	 *            the maximum capacity of a cached buffer. A buffer whose
	 *            capacity is bigger than this value is not pooled. <tt>0</tt>
	 *            disables this limitation.
	 */
	public CachedBufferAllocator(int maxPoolSize, int maxCachedBufferSize) {
		if (maxPoolSize < 0) {
			throw new IllegalArgumentException("maxPoolSize: " + maxPoolSize);
		}
		if (maxCachedBufferSize < 0) {
			throw new IllegalArgumentException("maxCachedBufferSize: "
					+ maxCachedBufferSize);
		}

		this.maxPoolSize = maxPoolSize;
		this.maxCachedBufferSize = maxCachedBufferSize;

		this.heapBuffers = new ThreadLocal<Map<Integer, Queue<CachedByteBufferWrapper>>>() {
			@Override
			protected Map<Integer, Queue<CachedByteBufferWrapper>> initialValue() {
				return newPoolMap();
			}
		};
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public int getMaxCachedBufferSize() {
		return maxCachedBufferSize;
	}

	/**
	 * 初始化缓冲池
	 * 
	 * @return
	 */
	private Map<Integer, Queue<CachedByteBufferWrapper>> newPoolMap() {
		Map<Integer, Queue<CachedByteBufferWrapper>> poolMap = new HashMap<Integer, Queue<CachedByteBufferWrapper>>();
		int poolSize = maxPoolSize == 0 ? DEFAULT_MAX_POOL_SIZE : maxPoolSize;
		for (int i = 0; i < 31; i++) {
			poolMap.put(1 << i, new CircularQueue<CachedByteBufferWrapper>(
					poolSize));
		}
		poolMap.put(0, new CircularQueue<CachedByteBufferWrapper>(poolSize));
		poolMap.put(Integer.MAX_VALUE,
				new CircularQueue<CachedByteBufferWrapper>(poolSize));
		return poolMap;
	}

	public ByteBufferWrapper allocate(int requestedCapacity) {
		// 圆整requestedCapacity到2的x次方
		int actualCapacity = ByteUtils.normalizeCapacity(requestedCapacity);
		ByteBufferWrapper buf;
		if (maxCachedBufferSize != 0 && actualCapacity > maxCachedBufferSize) {
			buf = wrap(ByteBuffer.allocate(actualCapacity));
		} else {
			Queue<CachedByteBufferWrapper> pool;
			pool = heapBuffers.get().get(actualCapacity);
			// 从池中取
			buf = pool.poll();
			if (buf != null) {
				buf.clear();
			} else {
				buf = wrap(ByteBuffer.allocate(actualCapacity));
			}
		}
		buf.limit(requestedCapacity);
		return buf;
	}

	public ByteBufferWrapper wrap(ByteBuffer nioBuffer) {
		return new CachedByteBufferWrapper(nioBuffer);
	}

	public void dispose() {
	}

	public class CachedByteBufferWrapper implements ByteBufferWrapper {

		Thread ownerThread; // 所分配的线程
		ByteBuffer origBuffer;

		public CachedByteBufferWrapper(ByteBuffer origBuffer) {
			super();
			this.ownerThread = Thread.currentThread();
			this.origBuffer = origBuffer;
		}

		@Override
		public void free() {
			if (origBuffer == null
					|| origBuffer.capacity() > maxCachedBufferSize
					|| Thread.currentThread() != ownerThread) {
				return;
			}

			// Add to the cache.
			Queue<CachedByteBufferWrapper> pool;
			pool = heapBuffers.get().get(origBuffer.capacity());
			if (pool == null) {
				return;
			}
			// 防止OOM
			if (maxPoolSize == 0 || pool.size() < maxPoolSize) {
				pool.offer(new CachedByteBufferWrapper(origBuffer));
			}

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

}
