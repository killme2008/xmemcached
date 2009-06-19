// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

/**
 * Transcoder that serializes and unserializes longs.
 */
public final class IntegerTranscoder 
	implements Transcoder<Integer> {

	private static final int flags = SerializingTranscoder.SPECIAL_INT;

	private final TranscoderUtils tu=new TranscoderUtils(true);

	public CachedData encode(java.lang.Integer l) {
		return new CachedData(flags, tu.encodeInt(l));
	}

	public Integer decode(CachedData d) {
		if (flags == d.getFlags()) {
			return tu.decodeInt(d.getData());
		} else {
			return null;
		}
	}

}
