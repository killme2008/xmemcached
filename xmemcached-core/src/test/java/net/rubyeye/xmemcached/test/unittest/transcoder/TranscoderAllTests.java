package net.rubyeye.xmemcached.test.unittest.transcoder;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TranscoderAllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for net.rubyeye.xmemcached.test.unittest.transcoder");
		// $JUnit-BEGIN$
		suite.addTestSuite(WhalinV1TranscoderTest.class);
		suite.addTestSuite(WhalinTranscoderTest.class);
		suite.addTestSuite(LongTranscoderTest.class);
		suite.addTestSuite(BaseSerializingTranscoderTest.class);
		suite.addTestSuite(SerializingTranscoderTest.class);
		suite.addTestSuite(TranscoderUtilsTest.class);
		suite.addTestSuite(IntegerTranscoderTest.class);
		suite.addTestSuite(CachedDataTest.class);
		suite.addTestSuite(PrimitiveAsStringUnitTest.class);
		// $JUnit-END$
		return suite;
	}

}
