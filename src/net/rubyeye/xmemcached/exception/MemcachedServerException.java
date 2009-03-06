package net.rubyeye.xmemcached.exception;

public class MemcachedServerException extends MemcachedException {

	public MemcachedServerException() {
		super();
	}

	public MemcachedServerException(String s) {
		super(s);
	}

	public MemcachedServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public MemcachedServerException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = -236562546568164115L;
}
