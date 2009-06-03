package net.rubyeye.xmemcached.codec;

import com.google.code.yanf4j.nio.CodecFactory;

import net.rubyeye.xmemcached.command.Command;

/**
 * Memcached protocol codec factory
 * 
 * @author dennis
 * 
 * @param <Command>
 */
public class MemcachedCodecFactory implements CodecFactory<Command> {

	private MemcachedTextEncoder encoder;

	private MemcachedTextDecoder decoder;

	public MemcachedCodecFactory() {
		super();
		this.encoder = new MemcachedTextEncoder();
		this.decoder = new MemcachedTextDecoder();
	}

	/**
	 * return the memcached protocol decoder
	 */
	@Override
	public final CodecFactory.Decoder<Command> getDecoder() {
		return this.decoder;

	}

	/**
	 * return the memcached protocol encoder
	 */
	@Override
	public final CodecFactory.Encoder<Command> getEncoder() {
		return this.encoder;
	}
}
