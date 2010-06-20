package com.google.code.yanf4j.test.unittest;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.google.code.yanf4j.test.unittest.config.ConfigurationTest;
import com.google.code.yanf4j.test.unittest.core.SessionFlowControllUnitTest;
import com.google.code.yanf4j.test.unittest.statistics.SimpleStatisticsTest;
import com.google.code.yanf4j.test.unittest.tcp.SessionIdleUnitTest;
import com.google.code.yanf4j.test.unittest.tcp.SessionTimeoutUnitTest;
import com.google.code.yanf4j.test.unittest.tcp.TCPControllerTest;
import com.google.code.yanf4j.test.unittest.utils.SystemUtilsUniTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for com.google.code.yanf4j.test.unittest");
		// $JUnit-BEGIN$
		suite.addTestSuite(SimpleStatisticsTest.class);
		suite.addTestSuite(ConfigurationTest.class);
		suite.addTestSuite(TCPControllerTest.class);
		suite.addTestSuite(SessionTimeoutUnitTest.class);
		suite.addTestSuite(SessionIdleUnitTest.class);
		suite.addTestSuite(SessionFlowControllUnitTest.class);
		suite.addTestSuite(SystemUtilsUniTest.class);

		// $JUnit-END$
		return suite;
	}

}
