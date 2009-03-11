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
