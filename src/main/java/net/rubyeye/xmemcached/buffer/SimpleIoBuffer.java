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

public class SimpleIoBuffer implements IoBuffer {

    protected ByteBuffer origBuffer;

    public SimpleIoBuffer(ByteBuffer origBuffer) {
        this.origBuffer = origBuffer;
    }

    @Override
    public final void free() {
        this.origBuffer = null;
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

	@Override
	public void order(ByteOrder byteOrder) {
		this.origBuffer.order(byteOrder);	
	}
    
    
}
