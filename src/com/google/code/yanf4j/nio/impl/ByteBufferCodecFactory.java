package com.google.code.yanf4j.nio.impl;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.nio.ByteBuffer;

import com.google.code.yanf4j.nio.CodecFactory;

public class ByteBufferCodecFactory implements CodecFactory<ByteBuffer> {

	static class ByteBufferDecoder implements Decoder<ByteBuffer> {

		public ByteBuffer decode(ByteBuffer buff) {
			byte[] bytes = new byte[buff.remaining()];
			buff.get(bytes);
			return ByteBuffer.wrap(bytes);
		}

	}

	private static Decoder<ByteBuffer> decoder = new ByteBufferDecoder();

	public Decoder<ByteBuffer> getDecoder() {
		return decoder;
	}

	static class ByteBufferEncoder implements Encoder<ByteBuffer> {

		public ByteBuffer[] encode(ByteBuffer message) {
			ByteBuffer buff = ByteBuffer.allocate(message.remaining());
			byte[] bytes = new byte[message.remaining()];
			message.get(bytes);
			buff.put(bytes);
			buff.flip();
			return new ByteBuffer[] { buff };
		}

	}

	private static Encoder<ByteBuffer> encoder = new ByteBufferEncoder();

	public Encoder<ByteBuffer> getEncoder() {
		return encoder;
	}

}
