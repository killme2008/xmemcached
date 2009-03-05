package com.google.code.yanf4j.nio.impl;

/**
 *Copyright [2008] [dennis zhuang]
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
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.util.ByteBufferPattern;
import com.google.code.yanf4j.util.ByteBufferUtils;

public class StringCodecFactory implements CodecFactory<String> {

	private static final ByteBuffer SPLIT = ByteBuffer.wrap("\r\n".getBytes());

	private static final ByteBufferPattern SPLIT_PATTERN = ByteBufferPattern
			.compile(SPLIT);

	private static final String DEFAULT_CHARSET_NAME = "utf-8";

	private Charset charset;

	public StringCodecFactory() {
		charset = Charset.forName(DEFAULT_CHARSET_NAME);
	}

	public StringCodecFactory(String charsetName) {
		charset = Charset.forName(charsetName);
	}

	class StringDecoder implements Decoder<String> {
		public String decode(ByteBuffer buffer) {
			String result = null;
			int index = ByteBufferUtils.kmpIndexOf(buffer, SPLIT_PATTERN);
			if (index >= 0) {
				int limit = buffer.limit();
				buffer.limit(index);
				CharBuffer charBuffer = charset.decode(buffer);
				result = charBuffer.toString();
				buffer.limit(limit);
				buffer.position(index + SPLIT.remaining());

			}
			return result;
		}
	}

	private Decoder<String> decoder = new StringDecoder();

	public Decoder<String> getDecoder() {
		return decoder;

	}

	class StringEncoder implements Encoder<String> {
		public ByteBuffer[] encode(String message) {
			ByteBuffer buff = charset.encode(message);
			return new ByteBuffer[] { buff, SPLIT.slice() };
		}

	}

	private Encoder<String> encoder = new StringEncoder();

	public Encoder<String> getEncoder() {
		return encoder;
	}

}
