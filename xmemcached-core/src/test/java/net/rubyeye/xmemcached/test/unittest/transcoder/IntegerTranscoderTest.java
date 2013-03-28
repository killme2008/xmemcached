package net.rubyeye.xmemcached.test.unittest.transcoder;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.IntegerTranscoder;
import junit.framework.TestCase;

/**
 * Test the integer transcoder.
 */
public class IntegerTranscoderTest extends TestCase {

	private IntegerTranscoder tc=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc=new IntegerTranscoder();
	}

	public void testInt() throws Exception {
		assertEquals(923, tc.decode(tc.encode(923)).intValue());
	}

	public void testBadFlags() throws Exception {
		CachedData cd=tc.encode(9284);
		assertNull(tc.decode(new CachedData(cd.getFlag()+1, cd.getData(),
				CachedData.MAX_SIZE,-1)));
	}
}
