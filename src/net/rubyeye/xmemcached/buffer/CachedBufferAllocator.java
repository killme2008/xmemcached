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
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import net.rubyeye.xmemcached.utils.ByteUtils;
import com.google.code.yanf4j.util.CircularQueue;

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
    private final ThreadLocal<Map<Integer, Queue<CachedIoBuffer>>> heapBuffers;

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
            throw new IllegalArgumentException("maxCachedBufferSize: " + maxCachedBufferSize);
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
    private Map<Integer, Queue<CachedIoBuffer>> newPoolMap() {
        Map<Integer, Queue<CachedIoBuffer>> poolMap = new HashMap<Integer, Queue<CachedIoBuffer>>();
        int poolSize = maxPoolSize == 0 ? DEFAULT_MAX_POOL_SIZE : maxPoolSize;
        for (int i = 0; i < 31; i++) {
            poolMap.put(1 << i, new CircularQueue<CachedIoBuffer>(
                    poolSize));
        }
        poolMap.put(0, new CircularQueue<CachedIoBuffer>(poolSize));
        poolMap.put(Integer.MAX_VALUE,
                new CircularQueue<CachedIoBuffer>(poolSize));
        return poolMap;
    }

    public IoBuffer allocate(int requestedCapacity) {
        // 圆整requestedCapacity到2的x次方
        int actualCapacity = ByteUtils.normalizeCapacity(requestedCapacity);
        IoBuffer buf;
        if (maxCachedBufferSize != 0 && actualCapacity > maxCachedBufferSize) {
            buf = wrap(ByteBuffer.allocate(actualCapacity));
        } else {
            Queue<CachedIoBuffer> pool;
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

    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new CachedIoBuffer(nioBuffer);
    }

    public void dispose() {
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

        @Override
        public final void free() {
            if (origBuffer == null || origBuffer.capacity() > maxCachedBufferSize || Thread.currentThread() != ownerThread) {
                return;
            }

            // Add to the cache.
            Queue<CachedIoBuffer> pool;
            pool = heapBuffers.get().get(origBuffer.capacity());
            if (pool == null) {
                return;
            }
            // 防止OOM
            if (maxPoolSize == 0 || pool.size() < maxPoolSize) {
                pool.offer(new CachedIoBuffer(origBuffer));
            }

        }

        @Override
        public final ByteBuffer[] getByteBuffers() {
            return new ByteBuffer[]{
                        this.origBuffer};
        }

        @Override
        public final void put(byte[] bytes) {
            this.origBuffer.put(bytes);
        }

        @Override
        public final int capacity() {
            return this.origBuffer.capacity();
        }

        @Override
        public final void clear() {
            this.origBuffer.clear();
        }

        @Override
        public final void reset() {
            origBuffer.reset();
        }

        @Override
        public final int remaining() {
            return origBuffer.remaining();
        }

        @Override
        public final int position() {
            return origBuffer.position();
        }

        @Override
        public final void mark() {
            origBuffer.mark();
        }

        @Override
        public final int limit() {
            return origBuffer.limit();
        }

        @Override
        public final boolean hasRemaining() {
            return origBuffer.hasRemaining();
        }

        @Override
        public final void flip() {
            this.origBuffer.flip();
        }

        @Override
        public final void put(byte b) {
            this.origBuffer.put(b);
        }

        @Override
        public final void put(ByteBuffer buff) {
            this.origBuffer.put(buff);
        }

        @Override
        public final ByteBuffer getByteBuffer() {
            return this.origBuffer;
        }

        @Override
        public final void limit(int limit) {
            this.origBuffer.limit(limit);
        }

        @Override
        public final void position(int pos) {
            this.origBuffer.position(pos);
        }
    }
}
