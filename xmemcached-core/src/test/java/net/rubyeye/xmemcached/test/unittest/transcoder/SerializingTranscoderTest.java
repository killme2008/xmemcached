// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.rubyeye.xmemcached.test.unittest.transcoder;

import java.util.Arrays;
import java.util.Calendar;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.CompressionMode;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.TranscoderUtils;

/**
 * Test the serializing transcoder.
 */
public class SerializingTranscoderTest extends BaseTranscoderCase {

	private SerializingTranscoder tc;
	private TranscoderUtils tu;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc = new SerializingTranscoder();
		setTranscoder(tc);
		tu = new TranscoderUtils(true);
	}

	public void testNonserializable() throws Exception {
		try {
			tc.encode(new Object());
			fail("Processed a non-serializable object.");
		} catch (IllegalArgumentException e) {
			// pass
		}
	}

	public void testCompressedStringNotSmaller() throws Exception {
		String s1 = "This is a test simple string that will not be compressed.";
		// Reduce the compression threshold so it'll attempt to compress it.
		tc.setCompressionThreshold(8);
		CachedData cd = tc.encode(s1);
		// This should *not* be compressed because it is too small
		assertEquals(0, cd.getFlag());
		assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testCompressedString() throws Exception {
		// This one will actually compress
		String s1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		tc.setCompressionThreshold(8);
		CachedData cd = tc.encode(s1);
		assertEquals(SerializingTranscoder.COMPRESSED, cd.getFlag());
		assertFalse(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testObject() throws Exception {
		Calendar c = Calendar.getInstance();
		CachedData cd = tc.encode(c);
		assertEquals(SerializingTranscoder.SERIALIZED, cd.getFlag());
		assertEquals(c, tc.decode(cd));
	}

	public void testCompressedObject() throws Exception {
		tc.setCompressionThreshold(8);
		Calendar c = Calendar.getInstance();
		CachedData cd = tc.encode(c);
		assertEquals(SerializingTranscoder.SERIALIZED
				| SerializingTranscoder.COMPRESSED, cd.getFlag());
		assertEquals(c, tc.decode(cd));
	}

	public void testCompressedObjectDecompressZipMode() throws Exception {
		tc.setCompressionThreshold(8);
		tc.setCompressionMode(CompressionMode.ZIP);
		Calendar c = Calendar.getInstance();
		CachedData cd = tc.encode(c);
		assertEquals(SerializingTranscoder.SERIALIZED
				| SerializingTranscoder.COMPRESSED, cd.getFlag());
		assertEquals(c, tc.decode(cd));
		tc.setCompressionMode(CompressionMode.GZIP);
	}

	public void testUnencodeable() throws Exception {
		try {
			CachedData cd = tc.encode(new Object());
			fail("Should fail to serialize, got" + cd);
		} catch (IllegalArgumentException e) {
			// pass
		}
	}

	public void testUndecodeable() throws Exception {
		CachedData cd = new CachedData(
				Integer.MAX_VALUE
						& ~(SerializingTranscoder.COMPRESSED | SerializingTranscoder.SERIALIZED),
				tu.encodeInt(Integer.MAX_VALUE), tc.getMaxSize(), -1);
		assertNull(tc.decode(cd));
	}

	public void testUndecodeableSerialized() throws Exception {
		CachedData cd = new CachedData(SerializingTranscoder.SERIALIZED,
				tu.encodeInt(Integer.MAX_VALUE), tc.getMaxSize(), -1);
		assertNull(tc.decode(cd));
	}

	public void testUndecodeableCompressed() throws Exception {
		CachedData cd = new CachedData(SerializingTranscoder.COMPRESSED,
				tu.encodeInt(Integer.MAX_VALUE), tc.getMaxSize(), -1);
		System.out.println("got " + tc.decode(cd));
		assertNull(tc.decode(cd));
	}

	@Override
	protected int getStringFlags() {
		return 0;
	}

}
