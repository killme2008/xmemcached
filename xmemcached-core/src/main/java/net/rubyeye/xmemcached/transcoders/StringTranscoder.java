package net.rubyeye.xmemcached.transcoders;

import java.io.UnsupportedEncodingException;

/**
 * String Transcoder
 * 
 * @author dennis
 * 
 */
public class StringTranscoder extends PrimitiveTypeTranscoder<String> {

	private String charset = BaseSerializingTranscoder.DEFAULT_CHARSET;

	public StringTranscoder(String charset) {
		this.charset = charset;
	}

	public StringTranscoder() {
		this(BaseSerializingTranscoder.DEFAULT_CHARSET);
	}

	public String decode(CachedData d) {
		if (d.getFlag() == 0) {
			String rv = null;
			try {
				if (d.getData() != null) {
					rv = new String(d.getData(), this.charset);
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			return rv;
		} else {
			throw new RuntimeException("Decode String error");
		}
	}

	public static final int STRING_FLAG = 0;

	public CachedData encode(String o) {
		byte[] b = null;

		try {
			b = o.getBytes(this.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return new CachedData(STRING_FLAG, b);
	}

}
