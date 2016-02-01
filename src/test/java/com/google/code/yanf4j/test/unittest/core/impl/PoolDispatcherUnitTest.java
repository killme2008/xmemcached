package com.google.code.yanf4j.test.unittest.core.impl;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.core.impl.PoolDispatcher;

/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ����11:34:43
 */

public class PoolDispatcherUnitTest {
	PoolDispatcher dispatcher;

	@Before
	public void setUp() {
		this.dispatcher = new PoolDispatcher(10, 60, TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy(), "test");
	}

	@After
	public void tearDown() {
		this.dispatcher.stop();
	}

	private static final class TestRunner implements Runnable {
		boolean ran;

		public void run() {
			this.ran = true;

		}
	}

	@Test
	public void testDispatch() throws Exception {
		TestRunner runner = new TestRunner();
		this.dispatcher.dispatch(runner);
		Thread.sleep(1000);
		Assert.assertTrue(runner.ran);

	}

	@Test(expected = NullPointerException.class)
	public void testDispatchNull() throws Exception {
		this.dispatcher.dispatch(null);
	}

	@Test
	public void testDispatcherStop() throws Exception {
        this.dispatcher.stop();
		TestRunner runner = new TestRunner();
		this.dispatcher.dispatch(runner);
		Thread.sleep(1000);
		Assert.assertFalse(runner.ran);
	}

	@Test(expected = RejectedExecutionException.class)
	public void testDispatchReject() throws Exception {
		this.dispatcher = new PoolDispatcher(1, 1, 1, 60, TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy(),
				"test");
		this.dispatcher.dispatch(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {

				}
			}
		});
		this.dispatcher.dispatch(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {

				}
			}
		});

		// Should throw a RejectedExecutionException
		this.dispatcher.dispatch(new TestRunner());
	}
}
