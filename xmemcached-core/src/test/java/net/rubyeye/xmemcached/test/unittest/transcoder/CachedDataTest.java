package net.rubyeye.xmemcached.test.unittest.transcoder;

import net.rubyeye.xmemcached.transcoders.CachedData;
import junit.framework.TestCase;

/**
 * Test a couple aspects of CachedData.
 */
public class CachedDataTest extends TestCase {

	public void testToString() throws Exception {
		String exp="{CachedData flags=13 data=[84, 104, 105, 115, 32, 105, "
			+ "115, 32, 97, 32, 115, 105, 109, 112, 108, 101, 32, 116, 101, "
			+ "115, 116, 32, 115, 116, 114, 105, 110, 103, 46]}";
		CachedData cd=new CachedData(13,
			"This is a simple test string.".getBytes("UTF-8"),
			CachedData.MAX_SIZE,-1);
		assertEquals(exp, String.valueOf(cd));
	}

}
