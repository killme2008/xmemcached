package net.rubyeye.xmemcached.codec;

import com.google.code.yanf4j.nio.CodecFactory;

import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Memcached protocol codec factory
 *
 * @author dennis
 *
 * @param <Command>
 */
@SuppressWarnings("unchecked")
public abstract class MemcachedCodecFactory<Command> implements
		CodecFactory<Command> {

	public MemcachedCodecFactory() {
          this.transcoder=new SerializingTranscoder();
	}

	protected Transcoder transcoder;

	public MemcachedCodecFactory(Transcoder transcoder) {
		super();
		this.transcoder = transcoder;
	}

	public Transcoder getTranscoder() {
		return transcoder;
	}

	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

}
