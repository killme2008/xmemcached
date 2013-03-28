package net.rubyeye.xmemcached.test.unittest.transcoder;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.transcoders.WhalinTranscoder;

public class PrimitiveAsStringUnitTest extends TestCase {
	private SerializingTranscoder serializingTranscoder = new SerializingTranscoder();
	private WhalinTranscoder whalinTranscoder = new WhalinTranscoder();
	String str = "hello";
	int i = 1;
	byte b = (byte) 2;
	long l = 34930403040L;
	float f = 1.02f;
	double d = 3.14d;
	short s = 1024;
	Map<String, Integer> map = new HashMap<String, Integer>();

	@Override
	public void setUp() {
		this.serializingTranscoder.setPrimitiveAsString(true);
		this.whalinTranscoder.setPrimitiveAsString(true);
	}

	public void testXmemcachedTranscoderEncode() {

		Assert.assertEquals(this.str, encodeAndGet(this.str,
				this.serializingTranscoder));
		Assert.assertEquals("1", encodeAndGet(this.i,
				this.serializingTranscoder));
		Assert.assertEquals("2", encodeAndGet(this.b,
				this.serializingTranscoder));
		Assert.assertEquals("34930403040", encodeAndGet(this.l,
				this.serializingTranscoder));
		Assert.assertEquals("1.02", encodeAndGet(this.f,
				this.serializingTranscoder));
		Assert.assertEquals("3.14", encodeAndGet(this.d,
				this.serializingTranscoder));
//		Assert.assertEquals("1024", encodeAndGet(this.s,
//				this.serializingTranscoder));

	}

	public void testXmemcachedDecode() {
		Assert.assertEquals(this.str, decodeAndGet(this.str,
				this.serializingTranscoder));
		Assert.assertEquals("1", decodeAndGet(this.i,
				this.serializingTranscoder));
		Assert.assertEquals("2", decodeAndGet(this.b,
				this.serializingTranscoder));
		Assert.assertEquals("34930403040", decodeAndGet(this.l,
				this.serializingTranscoder));
		Assert.assertEquals("1.02", decodeAndGet(this.f,
				this.serializingTranscoder));
		Assert.assertEquals("3.14", decodeAndGet(this.d,
				this.serializingTranscoder));
		Assert.assertEquals(this.s, decodeAndGet(this.s,
				this.serializingTranscoder));
		Assert.assertEquals(this.map, decodeAndGet(this.map,
				this.serializingTranscoder));
	}

	public void testWhalinTranscoderTranscoderEncode() {

		Assert.assertEquals(this.str, encodeAndGet(this.str,
				this.whalinTranscoder));
		Assert.assertEquals("1", encodeAndGet(this.i, this.whalinTranscoder));
		Assert.assertEquals("2", encodeAndGet(this.b, this.whalinTranscoder));
		Assert.assertEquals("34930403040", encodeAndGet(this.l,
				this.whalinTranscoder));
		Assert
				.assertEquals("1.02", encodeAndGet(this.f,
						this.whalinTranscoder));
		Assert
				.assertEquals("3.14", encodeAndGet(this.d,
						this.whalinTranscoder));
		Assert
				.assertEquals("1024", encodeAndGet(this.s,
						this.whalinTranscoder));
	}

	public void testWhalinTranscoderDecode() {
		Assert.assertEquals(this.str, decodeAndGet(this.str,
				this.whalinTranscoder));
		Assert.assertEquals("1", decodeAndGet(this.i, this.whalinTranscoder));
		Assert.assertEquals("2", decodeAndGet(this.b, this.whalinTranscoder));
		Assert.assertEquals("34930403040", decodeAndGet(this.l,
				this.whalinTranscoder));
		Assert
				.assertEquals("1.02", decodeAndGet(this.f,
						this.whalinTranscoder));
		Assert
				.assertEquals("3.14", decodeAndGet(this.d,
						this.whalinTranscoder));
		Assert
				.assertEquals("1024", decodeAndGet(this.s,
						this.whalinTranscoder));
		Assert.assertEquals(this.map, decodeAndGet(this.map,
				this.whalinTranscoder));
	}

	private Object decodeAndGet(Object obj, Transcoder transcoder) {
		CachedData data = transcoder.encode(obj);
		Object decodeString = transcoder.decode(data);
		return decodeString;
	}

	private String encodeAndGet(Object obj, Transcoder transcoder) {
		CachedData data = encode(obj, transcoder);
		String encodeString = new String(data.getData());
		return encodeString;
	}

	private CachedData encode(Object str, Transcoder transcoder) {
		CachedData data = transcoder.encode(str);
		return data;
	}
}
