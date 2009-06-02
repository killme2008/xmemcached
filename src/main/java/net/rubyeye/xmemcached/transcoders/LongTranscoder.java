// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

import net.spy.SpyObject;


/**
 * Transcoder that serializes and unserializes longs.
 */
public final class LongTranscoder extends SpyObject
	implements Transcoder<Long> {

	private static final int flags = SerializingTranscoder.SPECIAL_LONG;

	private final TranscoderUtils tu=new TranscoderUtils(true);

	public CachedData encode(java.lang.Long l) {
		return new CachedData(flags, tu.encodeLong(l));
	}

	public Long decode(CachedData d) {
		if (flags == d.getFlags()) {
			return tu.decodeLong(d.getData());
		} else {
			getLogger().error("Unexpected flags for long:  "
				+ d.getFlags() + " wanted " + flags);
			return null;
		}
	}

}
