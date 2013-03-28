package net.rubyeye.xmemcached.helper;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.utils.Protocol;

public class TooLongKeyChecker extends AbstractChecker {
	private MemcachedClient client;
	

	public TooLongKeyChecker(MemcachedClient client) {
		super();
		this.client = client;
	}


	public void check() throws Exception {
		try {
			call();
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(
					"Key is too long (maxlen = "
							+ (this.client.getProtocol() == Protocol.Text ? 250
									: 250) + ")", e.getMessage());
		}

	}

}
