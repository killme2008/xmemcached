package net.rubyeye.xmemcached.utils;

import java.net.InetSocketAddress;

/**
 * InetSocketAddress wrapper,encapsulate an order number.
 * 
 * @author dennis
 * 
 */
public class InetSocketAddressWrapper {
	private InetSocketAddress inetSocketAddress;
	private int order;

	public InetSocketAddressWrapper(InetSocketAddress inetSocketAddress,
			int order) {
		super();
		this.inetSocketAddress = inetSocketAddress;
		this.order = order;
	}

	public final InetSocketAddress getInetSocketAddress() {
		return this.inetSocketAddress;
	}

	public final void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}

	public final int getOrder() {
		return this.order;
	}

	public final void setOrder(int order) {
		this.order = order;
	}

}
