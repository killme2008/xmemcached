package net.rubyeye.xmemcached.test.unittest.transcoder;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.LongTranscoder;
import junit.framework.TestCase;

/**
 * Test the long transcoder.
 */
public class LongTranscoderTest extends TestCase {

	private LongTranscoder tc=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc=new LongTranscoder();
	}

	public void testLong() throws Exception {
		assertEquals(923, tc.decode(tc.encode(923L)).longValue());
	}

	public void testBadFlags() throws Exception {
		CachedData cd=tc.encode(9284L);
		assertNull(tc.decode(new CachedData(cd.getFlag()+1, cd.getData(),
				CachedData.MAX_SIZE,-1)));
	}
}
