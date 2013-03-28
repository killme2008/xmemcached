package net.rubyeye.xmemcached.helper;

public class InValidKeyChecker extends AbstractChecker {

	public void check() throws Exception {
		try {
			call();
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith(
					"Key contains invalid characters"));
		}

	}

}
