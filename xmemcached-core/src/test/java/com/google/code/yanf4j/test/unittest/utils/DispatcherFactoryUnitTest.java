package com.google.code.yanf4j.test.unittest.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.util.DispatcherFactory;

public class DispatcherFactoryUnitTest {
	@Test
	public void testNewDispatcher() throws Exception {
		Dispatcher dispatcher = DispatcherFactory.newDispatcher(1,
				new ThreadPoolExecutor.AbortPolicy(),"test");
		Assert.assertNotNull(dispatcher);
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger count = new AtomicInteger();
		dispatcher.dispatch(new Runnable() {
			public void run() {
				count.incrementAndGet();
				latch.countDown();
			}
		});
		latch.await();
		Assert.assertEquals(1, count.get());

		Assert.assertNull(DispatcherFactory.newDispatcher(0,
				new ThreadPoolExecutor.AbortPolicy(),"test"));
		Assert.assertNull(DispatcherFactory.newDispatcher(-1,
				new ThreadPoolExecutor.AbortPolicy(),"test"));
		dispatcher.stop();
		try {
			dispatcher.dispatch(new Runnable() {
				public void run() {
					Assert.fail();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
