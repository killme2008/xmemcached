package com.google.code.yanf4j.test.unittest.core.impl;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.impl.AbstractController;
import com.google.code.yanf4j.core.impl.ByteBufferCodecFactory;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.core.impl.StandardSocketOption;
import com.google.code.yanf4j.nio.TCPController;

/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ����11:04:49
 */

public abstract class AbstractControllerUnitTest {
	protected AbstractController controller;

	@Test
	public void testConfigThreadCount() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setReadThreadCount(10);
		configuration.setWriteThreadCount(1);
		configuration.setDispatchMessageThreadCount(11);
		this.controller = new TCPController(configuration);
		this.controller.setHandler(new HandlerAdapter());
		Assert.assertEquals(10, this.controller.getReadThreadCount());
		Assert.assertEquals(1, this.controller.getWriteThreadCount());
		Assert.assertEquals(11,
				this.controller.getDispatchMessageThreadCount());

		this.controller.setReadThreadCount(0);
		this.controller.setWriteThreadCount(0);
		this.controller.setDispatchMessageThreadCount(0);

		Assert.assertEquals(0, this.controller.getReadThreadCount());
		Assert.assertEquals(0, this.controller.getWriteThreadCount());
		Assert.assertEquals(0, this.controller.getDispatchMessageThreadCount());
		try {
			this.controller.setReadThreadCount(-1);
			Assert.fail();
		} catch (IllegalArgumentException e) {

		}
		try {
			this.controller.setWriteThreadCount(-1);
			Assert.fail();
		} catch (IllegalArgumentException e) {

		}
		try {
			this.controller.setDispatchMessageThreadCount(-1);
			Assert.fail();
		} catch (IllegalArgumentException e) {

		}
		this.controller.start();
		try {
			this.controller.setReadThreadCount(1);
			Assert.fail();
		} catch (IllegalStateException e) {

		}

		try {
			this.controller.setWriteThreadCount(1);
			Assert.fail();
		} catch (IllegalStateException e) {

		}

		try {
			this.controller.setDispatchMessageThreadCount(1);
			Assert.fail();
		} catch (IllegalStateException e) {

		}

	}

	@Test
	public void testSetSocketOption() throws Exception {
		this.controller = new TCPController(new Configuration());
		this.controller.setSocketOption(StandardSocketOption.SO_KEEPALIVE,
				true);
		Assert.assertEquals(true, this.controller
				.getSocketOption(StandardSocketOption.SO_KEEPALIVE));

		this.controller.setSocketOption(StandardSocketOption.SO_RCVBUF, 4096);
		Assert.assertEquals((Integer) 4096, this.controller
				.getSocketOption(StandardSocketOption.SO_RCVBUF));

		try {
			this.controller.setSocketOption(null, 3);
			Assert.fail();
		} catch (NullPointerException e) {
			Assert.assertEquals("Null socketOption", e.getMessage());
		}
		try {
			this.controller.setSocketOption(StandardSocketOption.SO_RCVBUF,
					null);
			Assert.fail();
		} catch (NullPointerException e) {
			Assert.assertEquals("Null value", e.getMessage());
		}
	}

	@Test
	public void testNoHandler() throws Exception {
		this.controller = new TCPController(new Configuration());
		Assert.assertNull(this.controller.getHandler());
		try {
			this.controller.start();
			Assert.fail();
		} catch (IOException e) {
			Assert.assertEquals("The handler is null", e.getMessage());
		}
	}

	@Test
	public void testNoCodecFactory() throws Exception {
		this.controller = new TCPController(new Configuration());
		this.controller.setHandler(new HandlerAdapter());
		Assert.assertNull(this.controller.getCodecFactory());
		this.controller.start();
		Assert.assertTrue(this.controller
				.getCodecFactory() instanceof ByteBufferCodecFactory);
	}

	@Test
	public void testConfig() {
		Configuration configuration = new Configuration();
		this.controller = new TCPController(configuration);
		Assert.assertEquals(configuration.isHandleReadWriteConcurrently(),
				this.controller.isHandleReadWriteConcurrently());
		this.controller.setHandleReadWriteConcurrently(false);
		Assert.assertFalse(this.controller.isHandleReadWriteConcurrently());

		this.controller.setSessionIdleTimeout(100000);
		Assert.assertEquals(100000, this.controller.getSessionIdleTimeout());
		this.controller.setSessionTimeout(5000);
		Assert.assertEquals(5000, this.controller.getSessionTimeout());

		this.controller.setSoTimeout(9000);
		Assert.assertEquals(9000, this.controller.getSoTimeout());

	}

	@After
	public void tearDown() throws Exception {
		if (this.controller != null) {
			this.controller.stop();
		}
	}

}
