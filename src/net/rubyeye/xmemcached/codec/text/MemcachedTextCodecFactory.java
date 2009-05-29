package net.rubyeye.xmemcached.codec.text;

import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
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
public class MemcachedTextCodecFactory extends MemcachedCodecFactory<Command> {

	private MemcachedTextEncoder encoder;

	private MemcachedTextDecoder decoder;

	public MemcachedTextCodecFactory() {
		super();
		this.encoder = new MemcachedTextEncoder();
		this.decoder = new MemcachedTextDecoder(new StatisticsHandler(),
				this.transcoder);
	}

	public void setTranscoder(Transcoder transcoder) {
		super.setTranscoder(transcoder);
		this.decoder.setTranscoder(transcoder);
	}

	public MemcachedTextCodecFactory(Transcoder transcoder) {
		super(transcoder);
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
