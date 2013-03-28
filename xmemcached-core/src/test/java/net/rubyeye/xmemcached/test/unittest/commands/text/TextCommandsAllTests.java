package net.rubyeye.xmemcached.test.unittest.commands.text;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TextCommandsAllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for net.rubyeye.xmemcached.test.unittest.commands.text");
		//$JUnit-BEGIN$
		suite.addTestSuite(TextStatsCommandUnitTest.class);
		suite.addTestSuite(TextIncrDecrCommandUnitTest.class);
		suite.addTestSuite(TextVersionCommandUnitTest.class);
		suite.addTestSuite(TextStoreCommandUnitTest.class);
		suite.addTestSuite(TextFlushAllCommandUnitTest.class);
		suite.addTestSuite(TextDeleteCommandUnitTest.class);
		suite.addTestSuite(TextGetOneCommandUnitTest.class);
		suite.addTestSuite(TextGetMultiCommandUnitTest.class);
		//$JUnit-END$
		return suite;
	}

}
