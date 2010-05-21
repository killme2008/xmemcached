package net.rubyeye.xmemcached.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * A callback handler for name/password authentication
 * 
 * @author dennis
 * 
 */
public class PlainCallbackHandler implements CallbackHandler {
	private String username;
	private String password;
	
	

	public PlainCallbackHandler(String username, String password) {
		super();
		this.username = username;
		this.password = password;
	}



	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback) {
				((NameCallback) callback).setName(this.username);
			} else if (callback instanceof PasswordCallback) {
				((PasswordCallback) callback).setPassword(password
						.toCharArray());
			} else
				throw new UnsupportedCallbackException(callback);
		}

	}

}
