package net.rubyeye.xmemcached.helper;

public class BlankKeyChecker extends AbstractChecker {

	public void check() throws Exception {
		try {
			call();
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Key must not be blank", e.getMessage());
		}
	}

}
