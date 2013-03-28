// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transcoder that serializes and unserializes longs.
 */
public final class LongTranscoder extends PrimitiveTypeTranscoder<Long> {
	private static final Logger log = LoggerFactory
			.getLogger(LongTranscoder.class);

	public CachedData encode(java.lang.Long l) {
		/**
		 * store Long as string
		 */
		if (this.primitiveAsString) {
			byte[] b = encodeString(l.toString());
			int flags = 0;
			if (b.length > this.compressionThreshold) {
				byte[] compressed = compress(b);
				if (compressed.length < b.length) {
					if (log.isDebugEnabled()) {
						log.debug("Compressed " + l.getClass().getName()
								+ " from " + b.length + " to "
								+ compressed.length);
					}
					b = compressed;
					flags |= SerializingTranscoder.COMPRESSED;
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Compression increased the size of "
								+ l.getClass().getName() + " from " + b.length
								+ " to " + compressed.length);
					}
				}
			}
			return new CachedData(flags, b, b.length, -1);
		}
		return new CachedData(SerializingTranscoder.SPECIAL_LONG, this.tu.encodeLong(l));
	}

	public Long decode(CachedData d) {
		if (this.primitiveAsString) {
			byte[] data = d.getData();
			if ((d.getFlag() & SerializingTranscoder.COMPRESSED) != 0) {
				data = decompress(d.getData());
			}
			int flag = d.getFlag();
			if (flag == 0) {
				return Long.valueOf(decodeString(data));
			} else {
				return null;
			}
		} else {
			if (SerializingTranscoder.SPECIAL_LONG == d.getFlag()) {
				return this.tu.decodeLong(d.getData());
			} else {
				log.error("Unexpected flags for long:  " + d.getFlag()
						+ " wanted " + SerializingTranscoder.SPECIAL_LONG);
				return null;
			}
		}
	}

}
