package net.rubyeye.xmemcached;

import java.util.concurrent.Semaphore;

/**
 * Flow control for noreply operations.
 * 
 * @author dennis<killme2008@gmail.com>
 * @since 1.3.8
 * 
 */
public class FlowControl {
	private Semaphore permits;
	private int max;

	public FlowControl(int permits) {
		super();
		this.max = permits;
		this.permits = new Semaphore(permits);
	}

	public int max() {
		return this.max;
	}

	public int permits() {
		return this.permits.availablePermits();
	}

	public boolean aquire() {
		return this.permits.tryAcquire();
	}

	public void release() {
		this.permits.release();
	}
}
