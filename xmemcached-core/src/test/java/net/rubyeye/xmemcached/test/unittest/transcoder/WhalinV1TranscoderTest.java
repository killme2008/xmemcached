package net.rubyeye.xmemcached.test.unittest.transcoder;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.WhalinV1Transcoder;


public class WhalinV1TranscoderTest extends BaseTranscoderCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setTranscoder(new WhalinV1Transcoder());
	}

	@Override
	public void testByteArray() throws Exception {
		byte[] a = { 'a', 'b', 'c' };
		try {
			CachedData cd = getTranscoder().encode(a);
			fail("Expected IllegalArgumentException, got " + cd);
		} catch (IllegalArgumentException e) {
			// pass
		}
	}

	@Override
	protected int getStringFlags() {
		// Flags are not used by this transcoder.
		return 0;
	}

}
