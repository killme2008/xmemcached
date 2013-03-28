package net.rubyeye.xmemcached.auth;

import javax.security.auth.callback.CallbackHandler;

/**
 * Authentication infomation for a memcached server
 * 
 * @author dennis
 * 
 */
public class AuthInfo {
	private final CallbackHandler callbackHandler;
	private final String[] mechanisms;
	private final int maxAttempts = Integer.parseInt(System.getProperty(
			"net.rubyeye.xmemcached.auth_max_attempts", "-1"));
	private int attempts;

	public synchronized boolean isValid() {
		return this.attempts <= this.maxAttempts || this.maxAttempts < 0;
	}

	public synchronized boolean isFirstTime() {
		return this.attempts == 0;
	}

	public synchronized void increaseAttempts() {
		this.attempts++;
	}

	public AuthInfo(CallbackHandler callbackHandler, String[] mechanisms) {
		super();
		this.callbackHandler = callbackHandler;
		this.mechanisms = mechanisms;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	/**
	 * Get a typical auth descriptor for PLAIN auth with the given username and
	 * password.
	 * 
	 * @param u
	 *            the username
	 * @param p
	 *            the password
	 * 
	 * @return an AuthInfo
	 */
	public static AuthInfo plain(String username, String password) {
		return new AuthInfo(new PlainCallbackHandler(username, password),
				new String[] { "PLAIN" });
	}

	/**
	 * Get a typical auth descriptor for CRAM-MD5 auth with the given username
	 * and password.
	 * 
	 * @param u
	 *            the username
	 * @param p
	 *            the password
	 * 
	 * @return an AuthInfo
	 */
	public static AuthInfo cramMD5(String username, String password) {
		return new AuthInfo(new PlainCallbackHandler(username, password),
				new String[] { "CRAM-MD5" });
	}

	/**
	 * Get a typical auth descriptor for CRAM-MD5 or PLAIN auth with the given
	 * username and password.
	 * 
	 * @param u
	 *            the username
	 * @param p
	 *            the password
	 * 
	 * @return an AuthInfo
	 */
	public static AuthInfo typical(String username, String password) {
		return new AuthInfo(new PlainCallbackHandler(username, password),
				new String[] { "CRAM-MD5", "PLAIN" });
	}

	public CallbackHandler getCallbackHandler() {
		return callbackHandler;
	}

	public String[] getMechanisms() {
		return mechanisms;
	}

}
