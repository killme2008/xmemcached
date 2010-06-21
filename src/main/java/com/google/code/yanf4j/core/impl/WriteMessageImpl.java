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
package com.google.code.yanf4j.core.impl;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.WriteMessage;

/**
 * Write message implementation with a buffer
 * 
 * @author dennis
 * 
 */
public class WriteMessageImpl implements WriteMessage {

	protected Object message;

	protected IoBuffer buffer;

	protected FutureImpl<Boolean> writeFuture;

	protected volatile boolean writing;

	public final void writing() {
		this.writing = true;
	}

	public final boolean isWriting() {
		return this.writing;
	}

	public WriteMessageImpl(Object message, FutureImpl<Boolean> writeFuture) {
		this.message = message;
		this.writeFuture = writeFuture;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.code.yanf4j.nio.IWriteMessage#getBuffers()
	 */
	public synchronized final IoBuffer getWriteBuffer() {
		return this.buffer;
	}

	public synchronized final void setWriteBuffer(IoBuffer buffers) {
		this.buffer = buffers;

	}

	public final FutureImpl<Boolean> getWriteFuture() {
		return this.writeFuture;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.code.yanf4j.nio.IWriteMessage#getMessage()
	 */
	public final Object getMessage() {
		return this.message;
	}
}