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
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Session;

/**
 * Default codec factory
 * 
 * @author dennis
 * 
 */
public class ByteBufferCodecFactory implements CodecFactory {
	static final IoBuffer EMPTY_BUFFER = IoBuffer.allocate(0);

	private boolean direct;

	public ByteBufferCodecFactory() {
		this(false);
	}

	public ByteBufferCodecFactory(boolean direct) {
		super();
		this.direct = direct;
		this.encoder = new ByteBufferEncoder();
		this.decoder = new ByteBufferDecoder();
	}

	public class ByteBufferDecoder implements Decoder {

		public Object decode(IoBuffer buff, Session session) {
			if (buff == null) {
				return null;
			}
			if (buff.remaining() == 0) {
				return EMPTY_BUFFER;
			}
			byte[] bytes = new byte[buff.remaining()];
			buff.get(bytes);
			IoBuffer result = IoBuffer.allocate(bytes.length,
					ByteBufferCodecFactory.this.direct);
			result.put(bytes);
			result.flip();
			return result;
		}

	}

	private Decoder decoder;

	public Decoder getDecoder() {
		return this.decoder;
	}

	public class ByteBufferEncoder implements Encoder {

		public IoBuffer encode(Object message, Session session) {
			final IoBuffer msgBuffer = (IoBuffer) message;
			if (msgBuffer == null) {
				return null;
			}
			if (msgBuffer.remaining() == 0) {
				return EMPTY_BUFFER;
			}
			byte[] bytes = new byte[msgBuffer.remaining()];
			msgBuffer.get(bytes);
			IoBuffer result = IoBuffer.allocate(bytes.length,
					ByteBufferCodecFactory.this.direct);
			result.put(bytes);
			result.flip();
			return result;
		}

	}

	private Encoder encoder;

	public Encoder getEncoder() {
		return this.encoder;
	}

}
