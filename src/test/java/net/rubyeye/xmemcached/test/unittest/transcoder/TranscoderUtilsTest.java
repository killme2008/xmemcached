package net.rubyeye.xmemcached.test.unittest.transcoder;
import java.util.Arrays;

import net.rubyeye.xmemcached.transcoders.TranscoderUtils;

import junit.framework.TestCase;

/**
 * Some test coverage for transcoder utils.
 */
public class TranscoderUtilsTest extends TestCase {

	private TranscoderUtils tu;
	byte[] oversizeBytes=new byte[16];

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tu=new TranscoderUtils(true);
	}

	public void testBooleanOverflow() {
		try {
			boolean b=tu.decodeBoolean(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testByteOverflow() {
		try {
			byte b=tu.decodeByte(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testIntOverflow() {
		try {
			int b=tu.decodeInt(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testLongOverflow() {
		try {
			long b=tu.decodeLong(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testPackedLong() {
		assertEquals("[1]", Arrays.toString(tu.encodeLong(1)));
	}

	public void testUnpackedLong() {
		assertEquals("[0, 0, 0, 0, 0, 0, 0, 1]",
			Arrays.toString(new TranscoderUtils(false).encodeLong(1)));
	}
}
