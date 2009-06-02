package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.util.Properties;

import com.google.code.yanf4j.util.ResourcesUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for net.rubyeye.xmemcached.test.unittest");
		// $JUnit-BEGIN$
		suite.addTestSuite(OptimezerTest.class);
		suite.addTestSuite(CommandFactoryTest.class);
		suite.addTestSuite(AddrUtilTest.class);
		suite.addTestSuite(SessionLocatorTest.class);
		try {
			Properties properties = ResourcesUtils
					.getResourceAsProperties("test.properties");
			if (properties.get("test.memcached.servers") != null) {
				suite.addTestSuite(XMemcachedClientTest.class);
			}
		} catch (IOException e) {
			System.err
					.println("If you want to run the XMemcachedClientTest,please set the config file test.properties");
		}
		// $JUnit-END$
		return suite;
	}
}
