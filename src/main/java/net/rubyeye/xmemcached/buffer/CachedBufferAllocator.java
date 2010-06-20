/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.util.CircularQueue;

/**
 * Cached IoBuffer allocator,cached buffer in ThreadLocal.
 * 
 * @author dennis
 * 
 */
@Deprecated
public class CachedBufferAllocator implements BufferAllocator {

	private static final int DEFAULT_MAX_POOL_SIZE = 8;
	private static final int DEFAULT_MAX_CACHED_BUFFER_SIZE = 1 << 18; // 256KB
	private final int maxPoolSize;
	private final int maxCachedBufferSize;
	private final ThreadLocal<Map<Integer, Queue<CachedIoBuffer>>> heapBuffers;
	private final IoBuffer EMPTY_IO_BUFFER = new CachedBufferAllocator.CachedIoBuffer(
			ByteBuffer.allocate(0));

	/**
	 * Creates a new instance with the default parameters ({@literal
	 * #DEFAULT_MAX_POOL_SIZE} and {@literal #DEFAULT_MAX_CACHED_BUFFER_SIZE}).
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

		this.heapBuffers = new ThreadLocal<Map<Integer, Queue<CachedIoBuffer>>>() {

			
			@Override
			protected Map<Integer, Queue<CachedIoBuffer>> initialValue() {
				return newPoolMap();
			}
		};
	}

	public int getMaxPoolSize() {
		return this.maxPoolSize;
	}

	public int getMaxCachedBufferSize() {
		return this.maxCachedBufferSize;
	}

	/**
	 * 初始化缓冲池
	 * 
	 * @return
	 */
	private Map<Integer, Queue<CachedIoBuffer>> newPoolMap() {
		Map<Integer, Queue<CachedIoBuffer>> poolMap = new HashMap<Integer, Queue<CachedIoBuffer>>();
		int poolSize = this.maxPoolSize == 0 ? DEFAULT_MAX_POOL_SIZE
				: this.maxPoolSize;
		for (int i = 0; i < 31; i++) {
			poolMap.put(1 << i, new CircularQueue<CachedIoBuffer>(poolSize));
		}
		poolMap.put(0, new CircularQueue<CachedIoBuffer>(poolSize));
		poolMap.put(Integer.MAX_VALUE, new CircularQueue<CachedIoBuffer>(
				poolSize));
		return poolMap;
	}

	public final IoBuffer allocate(int requestedCapacity) {
		if (requestedCapacity == 0) {
			return this.EMPTY_IO_BUFFER;
		}
		// 圆整requestedCapacity到2的x次方
		int actualCapacity = ByteUtils.normalizeCapacity(requestedCapacity);
		IoBuffer buf;
		if (this.maxCachedBufferSize != 0
				&& actualCapacity > this.maxCachedBufferSize) {
			buf = wrap(ByteBuffer.allocate(actualCapacity));
		} else {
			Queue<CachedIoBuffer> pool;
			pool = this.heapBuffers.get().get(actualCapacity);
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

	public final IoBuffer wrap(ByteBuffer nioBuffer) {
		return new CachedIoBuffer(nioBuffer);
	}

	public void dispose() {
		this.heapBuffers.remove();
	}

	public static BufferAllocator newInstance() {
		return new CachedBufferAllocator();
	}

	public static BufferAllocator newInstance(int maxPoolSize,
			int maxCachedBufferSize) {
		return new CachedBufferAllocator(maxPoolSize, maxCachedBufferSize);
	}

	public class CachedIoBuffer implements IoBuffer {

		Thread ownerThread; // 所分配的线程
		ByteBuffer origBuffer;

		public CachedIoBuffer(ByteBuffer origBuffer) {
			super();
			this.ownerThread = Thread.currentThread();
			this.origBuffer = origBuffer;
		}

		
		public void putInt(int i) {
			this.origBuffer.putInt(i);

		}

		
		public void putShort(short s) {
			this.origBuffer.putShort(s);
		}

		
		public ByteOrder order() {
			return this.origBuffer.order();
		}

		
		public boolean isDirect() {
			return this.origBuffer.isDirect();
		}

		
		public void order(ByteOrder byteOrder) {
			this.origBuffer.order(byteOrder);
		}
		
		public void putLong(long l) {
			this.origBuffer.putLong(l);

		}
		
		public final void free() {
			if (this.origBuffer == null
					|| this.origBuffer.capacity() > CachedBufferAllocator.this.maxCachedBufferSize
					|| Thread.currentThread() != this.ownerThread) {
				return;
			}

			// Add to the cache.
			Queue<CachedIoBuffer> pool;
			pool = CachedBufferAllocator.this.heapBuffers.get().get(
					this.origBuffer.capacity());
			if (pool == null) {
				return;
			}
			// 防止OOM
			if (CachedBufferAllocator.this.maxPoolSize == 0
					|| pool.size() < CachedBufferAllocator.this.maxPoolSize) {
				pool.offer(new CachedIoBuffer(this.origBuffer));
			}
			this.origBuffer = null;

		}

		
		public final ByteBuffer[] getByteBuffers() {
			return new ByteBuffer[] { this.origBuffer };
		}

		
		public final void put(byte[] bytes) {
			this.origBuffer.put(bytes);
		}

		
		public final int capacity() {
			return this.origBuffer.capacity();
		}

		
		public final void clear() {
			this.origBuffer.clear();
		}

		
		public final void reset() {
			this.origBuffer.reset();
		}

		
		public final int remaining() {
			return this.origBuffer.remaining();
		}

		
		public final int position() {
			return this.origBuffer.position();
		}

		
		public final void mark() {
			this.origBuffer.mark();
		}

		
		public final int limit() {
			return this.origBuffer.limit();
		}

		
		public final boolean hasRemaining() {
			return this.origBuffer.hasRemaining();
		}

		
		public final void flip() {
			this.origBuffer.flip();
		}

		
		public final void put(byte b) {
			this.origBuffer.put(b);
		}

		
		public final void put(ByteBuffer buff) {
			this.origBuffer.put(buff);
		}

		
		public final ByteBuffer getByteBuffer() {
			return this.origBuffer;
		}

		
		public final void limit(int limit) {
			this.origBuffer.limit(limit);
		}

		
		public final void position(int pos) {
			this.origBuffer.position(pos);
		}
	}
}
