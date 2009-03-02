package net.rubyeye.xmemcached.exception;

public class MemcachedException extends RuntimeException {

	public MemcachedException() {
		super();
	}

	public MemcachedException(String s) {
		super(s);
	}

	public MemcachedException(String message, Throwable cause) {
		super(message, cause);
	}

	public MemcachedException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = -136568012546568164L;
}
