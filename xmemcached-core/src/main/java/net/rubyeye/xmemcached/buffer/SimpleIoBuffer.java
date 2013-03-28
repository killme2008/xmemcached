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
/**
 * Simpe ByteBuffer Wrapper
 */
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
@Deprecated
public class SimpleIoBuffer implements IoBuffer {

	protected ByteBuffer origBuffer;

	public SimpleIoBuffer(ByteBuffer origBuffer) {
		this.origBuffer = origBuffer;
	}

	
	public final void free() {
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

	
	public void putInt(int i) {
		this.origBuffer.putInt(i);

	}

	
	public void putShort(short s) {
		this.origBuffer.putShort(s);
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

	
	public void order(ByteOrder byteOrder) {
		this.origBuffer.order(byteOrder);
	}

	
	public boolean isDirect() {
		return this.origBuffer.isDirect();
	}

	
	public ByteOrder order() {
		return this.origBuffer.order();
	}

	
	public void putLong(long l) {
		this.origBuffer.putLong(l);

	}

}
