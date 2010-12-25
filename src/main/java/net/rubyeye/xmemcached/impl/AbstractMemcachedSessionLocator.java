package net.rubyeye.xmemcached.impl;

import net.rubyeye.xmemcached.MemcachedSessionLocator;

/**
 * Abstract session locator
 * 
 * @author dennis
 * @date 2010-12-25
 */
public abstract class AbstractMemcachedSessionLocator implements
		MemcachedSessionLocator {

	protected boolean failureMode;

	public void setFailureMode(boolean failureMode) {
		this.failureMode = failureMode;

	}

}
