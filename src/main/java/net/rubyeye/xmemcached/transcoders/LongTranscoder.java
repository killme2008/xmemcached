// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transcoder that serializes and unserializes longs.
 */
public final class LongTranscoder implements Transcoder<Long> {
	private static final Logger log = LoggerFactory.getLogger(LongTranscoder.class);

	private static final int flags = SerializingTranscoder.SPECIAL_LONG;

	private final TranscoderUtils tu = new TranscoderUtils(true);

	public CachedData encode(java.lang.Long l) {
		return new CachedData(flags, this.tu.encodeLong(l));
	}

	public Long decode(CachedData d) {
		if (flags == d.getFlag()) {
			return this.tu.decodeLong(d.getData());
		} else {
			log.error("Unexpected flags for long:  " + d.getFlag()
					+ " wanted " + flags);
			return null;
		}
	}

}
