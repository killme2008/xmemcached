package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.util.Properties;

import net.rubyeye.xmemcached.test.unittest.codec.MemcachedDecoderUnitTest;
import net.rubyeye.xmemcached.test.unittest.codec.MemcachedEncoderUnitTest;
import net.rubyeye.xmemcached.test.unittest.commands.factory.TextCommandFactoryTest;
import net.rubyeye.xmemcached.test.unittest.commands.text.TextCommandsAllTests;
import net.rubyeye.xmemcached.test.unittest.impl.OptimezerTest;
import net.rubyeye.xmemcached.test.unittest.impl.SessionLocatorTest;
import net.rubyeye.xmemcached.test.unittest.monitor.StatisticsHandlerUnitTest;
import net.rubyeye.xmemcached.test.unittest.transcoder.TranscoderAllTests;
import net.rubyeye.xmemcached.test.unittest.utils.AddrUtilTest;

import com.google.code.yanf4j.util.ResourcesUtils;

import junit.framework.Test;
import junit.framework.TestSuite;
/**
 * Run this test suite with jvm args "-ea"
 * @author dennis
 *
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for net.rubyeye.xmemcached.test.unittest");
		// $JUnit-BEGIN$
		suite.addTestSuite(OptimezerTest.class);
		suite.addTestSuite(TextCommandFactoryTest.class);
		suite.addTestSuite(AddrUtilTest.class);
		suite.addTestSuite(SessionLocatorTest.class);
		suite.addTest(TextCommandsAllTests.suite());
		suite.addTest(TranscoderAllTests.suite());
		suite.addTestSuite(StatisticsHandlerUnitTest.class);
		suite.addTestSuite(MemcachedDecoderUnitTest.class);
		suite.addTestSuite(MemcachedEncoderUnitTest.class);
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
