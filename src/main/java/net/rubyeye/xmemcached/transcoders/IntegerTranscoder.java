// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.transcoders;

/**
 * Transcoder that serializes and unserializes longs.
 */
public final class IntegerTranscoder extends PrimitiveTypeTranscoder<Integer> {

	public CachedData encode(java.lang.Integer l) {
		/**
		 * store integer as string
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
		return new CachedData(SerializingTranscoder.SPECIAL_INT, this.tu
				.encodeInt(l));
	}

	public Integer decode(CachedData d) {
		if (this.primitiveAsString) {
			byte[] data = d.getData();
			if ((d.getFlag() & SerializingTranscoder.COMPRESSED) != 0) {
				data = decompress(d.getData());
			}
			int flag = d.getFlag();
			if (flag == 0) {
				return Integer.valueOf(decodeString(data));
			} else {
				return null;
			}
		} else {
			if (SerializingTranscoder.SPECIAL_INT == d.getFlag()) {
				return this.tu.decodeInt(d.getData());
			} else {
				return null;
			}
		}
	}

	@Override
	public void setPrimitiveAsString(boolean primitiveAsString) {
		this.primitiveAsString = primitiveAsString;
	}

	@Override
	public void setPackZeros(boolean packZeros) {
		this.tu.setPackZeros(packZeros);
	}

}
