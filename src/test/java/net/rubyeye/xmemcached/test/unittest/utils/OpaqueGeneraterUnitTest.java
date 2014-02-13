package net.rubyeye.xmemcached.test.unittest.utils;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import static junit.framework.Assert.*;
import net.rubyeye.xmemcached.utils.OpaqueGenerater;

public class OpaqueGeneraterUnitTest extends TestCase{

	public void testGetNextValue() throws Exception {
		OpaqueGenerater.getInstance().setValue(Integer.MAX_VALUE - 10000);
		final CyclicBarrier barrier = new CyclicBarrier(200 + 1);
		final AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>();
		for (int i = 0; i < 200; i++) {
			new Thread() {
				public void run() {
					try {
						barrier.await();
						for (int i = 0; i < 10000; i++) {
							if (OpaqueGenerater.getInstance().getNextValue() < 0)
								throw new RuntimeException("Test failed.");
						}
						barrier.await();
					} catch (Exception e) {
						exceptionRef.set(e);
						e.printStackTrace();
					}
				}
			}.start();
		}
		barrier.await();
		barrier.await();
		assertNull(exceptionRef.get());
		System.out.println(OpaqueGenerater.getInstance().getNextValue());

	}
}
