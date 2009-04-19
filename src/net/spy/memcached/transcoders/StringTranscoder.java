package net.spy.memcached.transcoders;

import java.io.UnsupportedEncodingException;

/**
 * String转换器，通常不建议直接使用这个类
 *
 * @author dennis
 *
 */
public class StringTranscoder implements Transcoder<String> {

	private String charset = BaseSerializingTranscoder.DEFAULT_CHARSET;

	public StringTranscoder(String charset) {
		this.charset = charset;
	}

	public StringTranscoder() {
		this(BaseSerializingTranscoder.DEFAULT_CHARSET);
	}

	@Override
	public String decode(CachedData d) {
		if (d.getFlags() == 0) {
			String rv = null;
			try {
				if (d.getData() != null) {
					rv = new String(d.getData(), charset);
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			return rv;
		} else
			throw new RuntimeException("Decode String error");
	}

	@Override
	public CachedData encode(String o) {
		byte[] b = null;
		int flags = 0;
		try {
			b = o.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return new CachedData(flags, b);
	}

}
