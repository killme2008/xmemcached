package net.rubyeye.xmemcached.test.unittest.commands.binary;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BinaryCommandAllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for net.rubyeye.xmemcached.test.unittest.commands.binary");
		//$JUnit-BEGIN$
		suite.addTestSuite(BinaryGetCommandUnitTest.class);
		suite.addTestSuite(BinaryDeleteCommandUnitTest.class);
		suite.addTestSuite(BinaryStoreCommandUnitTest.class);
		suite.addTestSuite(BinaryIncrDecrUnitTest.class);
		suite.addTestSuite(BinaryAppendPrependCommandUnitTest.class);
		suite.addTestSuite(BinaryStatsCommandUnitTest.class);
		suite.addTestSuite(BinaryGetMultiCommandUnitTest.class);
		suite.addTestSuite(BinaryCASCommandUnitTest.class);
		//$JUnit-END$
		return suite;
	}

}
