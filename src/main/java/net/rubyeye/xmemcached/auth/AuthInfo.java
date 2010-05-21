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

	public AuthInfo(CallbackHandler callbackHandler, String[] mechanisms) {
		super();
		this.callbackHandler = callbackHandler;
		this.mechanisms = mechanisms;
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
		return new AuthInfo(new PlainCallbackHandler(username,password),
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
		return new AuthInfo(new PlainCallbackHandler(username,password),
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
		return new AuthInfo(new PlainCallbackHandler(username,password), new String[] {
				"CRAM-MD5", "PLAIN" });
	}

	public CallbackHandler getCallbackHandler() {
		return callbackHandler;
	}

	public String[] getMechanisms() {
		return mechanisms;
	}

}
