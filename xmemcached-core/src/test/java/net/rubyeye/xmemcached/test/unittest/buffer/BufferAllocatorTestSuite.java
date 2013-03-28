package net.rubyeye.xmemcached.test.unittest.buffer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BufferAllocatorTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for net.rubyeye.xmemcached.test.unittest.buffer");
		//$JUnit-BEGIN$
		suite.addTestSuite(SimpleBufferAllocatorUnitTest.class);
		suite.addTestSuite(CachedBufferAllocatorUnitTest.class);
		//$JUnit-END$
		return suite;
	}

}
