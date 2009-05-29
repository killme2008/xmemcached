package net.rubyeye.xmemcached.codec.text;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.monitor.StatisticsHandler;
import net.rubyeye.xmemcached.transcoders.Transcoder;

import com.google.code.yanf4j.nio.CodecFactory;

/**
 * Memcached text protocol codec factory
 *
 * @author dennis
 *
 */
@SuppressWarnings("unchecked")
public class MemcachedTextCodecFactory implements CodecFactory<Command> {

	private Transcoder transcoder;

	private MemcachedTextEncoder encoder;

	private MemcachedTextDecoder decoder;

	public Transcoder getTranscoder() {
		return transcoder;
	}

	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
		this.decoder.setTranscoder(transcoder);
	}

	public MemcachedTextCodecFactory(Transcoder transcoder) {
		this.transcoder = transcoder;
		this.encoder = new MemcachedTextEncoder();
		this.decoder = new MemcachedTextDecoder(new StatisticsHandler(),
				this.transcoder);
	}

	/**
	 * return the memcached protocol decoder
	 */
	@Override
	public CodecFactory.Decoder<Command> getDecoder() {
		return this.decoder;

	}

	/**
	 * return the memcached protocol encoder
	 */
	@Override
	public CodecFactory.Encoder<Command> getEncoder() {
		return this.encoder;
	}

}
