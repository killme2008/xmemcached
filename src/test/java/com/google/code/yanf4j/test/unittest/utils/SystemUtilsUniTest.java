package com.google.code.yanf4j.test.unittest.utils;

import java.io.IOException;
import java.nio.channels.Selector;

import junit.framework.TestCase;

import com.google.code.yanf4j.util.SystemUtils;

public class SystemUtilsUniTest extends TestCase {
	public void testOpenSelector() throws IOException {
		Selector selector = SystemUtils.openSelector();
		assertNotNull(selector);
		assertTrue(selector.isOpen());
		if (SystemUtils.isLinuxPlatform()) {
			assertEquals(selector.provider().getClass().getCanonicalName(),
					"sun.nio.ch.EPollSelectorProvider");
		}
		Selector selector2 = SystemUtils.openSelector();
		;
		assertNotSame(selector, selector2);
		selector.close();
		selector2.close();
	}

}
