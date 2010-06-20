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

/**
 * Simple IoBuffer allocator,allcate a new heap ByteBuffer each time.
 * 
 * @author dennis
 * 
 */
@Deprecated
public class SimpleBufferAllocator implements BufferAllocator {

	public static final IoBuffer EMPTY_IOBUFFER = new SimpleIoBuffer(ByteBuffer
			.allocate(0));

	public final IoBuffer allocate(int capacity) {
		if (capacity == 0) {
			return EMPTY_IOBUFFER;
		} else {
			return wrap(ByteBuffer.allocate(capacity));
		}
	}

	public final void dispose() {
	}

	public final static BufferAllocator newInstance() {
		return new SimpleBufferAllocator();
	}

	
	public final IoBuffer wrap(ByteBuffer byteBuffer) {
		return new SimpleIoBuffer(byteBuffer);
	}

}
