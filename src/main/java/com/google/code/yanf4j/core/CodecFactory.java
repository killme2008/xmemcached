package com.google.code.yanf4j.core;

import com.google.code.yanf4j.buffer.IoBuffer;

/**
 * 
 * 
 * Codec factory
 * 
 * @author boyan
 * 
 */
public interface CodecFactory {

	public interface Encoder {
		public IoBuffer encode(Object message, Session session);
	}

	public interface Decoder {
		public Object decode(IoBuffer buff, Session session);
	}

	public Encoder getEncoder();

	public Decoder getDecoder();
}
