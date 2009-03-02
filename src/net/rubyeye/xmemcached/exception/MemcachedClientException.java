package net.rubyeye.xmemcached.exception;

public class MemcachedClientException extends RuntimeException {

	public MemcachedClientException() {
		super();
	}

	public MemcachedClientException(String s) {
		super(s);
	}

	public MemcachedClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public MemcachedClientException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = -236562546568164115L;
}
