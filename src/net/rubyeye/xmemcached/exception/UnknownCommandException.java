package net.rubyeye.xmemcached.exception;

public class UnknownCommandException extends RuntimeException {

	public UnknownCommandException() {
		super();
	}

	public UnknownCommandException(String s) {
		super(s);
	}

	public UnknownCommandException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownCommandException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 8736625460917630395L;
}
