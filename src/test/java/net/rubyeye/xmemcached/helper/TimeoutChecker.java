package net.rubyeye.xmemcached.helper;

import java.util.concurrent.TimeoutException;

public class TimeoutChecker extends AbstractChecker{
	private long timeout;
	

	public TimeoutChecker(long timeout) {
		super();
		this.timeout = timeout;
	}


	public void check() throws Exception {
		try {
			call();
			fail();
		} catch (TimeoutException e) {
			assertTrue(e.getMessage().startsWith(
					"Timed out(" + timeout
					+ ") waiting for operation"));
		}
		
	}
	

}
