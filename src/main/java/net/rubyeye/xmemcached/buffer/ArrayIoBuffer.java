/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rubyeye.xmemcached.buffer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author dennis
 */
@Deprecated
public class ArrayIoBuffer implements IoBuffer {

    public ByteBuffer[] byteBuffers;

    public ArrayIoBuffer(int initialCapacity) {
        this.byteBuffers = new ByteBuffer[initialCapacity];
    }

    public ArrayIoBuffer(List<ByteBuffer> bufferList) {
        byteBuffers = new ByteBuffer[bufferList.size()];
        bufferList.toArray(this.byteBuffers);
    }

    @Override
    public final int capacity() {
        int result = 0;
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                result += buffer.capacity();
            }
        }
        return result;
    }

    @Override
    public final void clear() {
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                buffer.clear();
            }
        }
    }

    @Override
    public final void flip() {
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                buffer.flip();
            }
        }
    }

    @Override
    public final void free() {
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                buffer.clear();
            }
        }
    }

    public final IoBuffer gathering(IoBuffer gatheringBuffer) {
        for (ByteBuffer buffer : this.byteBuffers) {
            if (buffer != null) {
                gatheringBuffer.put(buffer);
            }
        }
        return gatheringBuffer;
    }

    @Override
    public final ByteBuffer getByteBuffer() {
        ByteBuffer result = ByteBuffer.allocate(this.remaining());
        for (ByteBuffer buffer : this.byteBuffers) {
            if (buffer != null) {
                result.put(buffer);
            }
        }
        result.flip();
        return result;
    }

    @Override
    public final ByteBuffer[] getByteBuffers() {
        return this.byteBuffers;
    }

    @Override
    public final boolean hasRemaining() {
        for (ByteBuffer buffer : this.byteBuffers) {
            if (buffer != null && buffer.hasRemaining()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final int limit() {
        int result = 0;
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                result += buffer.limit();
            }
        }
        return result;
    }

    @Override
    public final void limit(int limit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final void mark() {
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                buffer.mark();
            }
        }
    }

    @Override
    public final int position() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final void position(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final void put(ByteBuffer buff) {
        boolean success = false;
        for (int i = 0; i < this.byteBuffers.length; i++) {
            if (this.byteBuffers[i] == null) {
                this.byteBuffers[i] = buff;
                success = true;
                break;
            }
        }
        if (!success) {
            this.byteBuffers = Arrays.copyOf(this.byteBuffers, this.byteBuffers.length + 10);
            this.byteBuffers[this.byteBuffers.length - 1] = buff;
        }
    }

    @Override
    public final void put(byte b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final void put(byte[] bytes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final int remaining() {
        int result = 0;
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                result += buffer.remaining();
            }
        }
        return result;
    }

    @Override
    public final void reset() {
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer != null) {
                buffer.reset();
            }
        }
    }
}
