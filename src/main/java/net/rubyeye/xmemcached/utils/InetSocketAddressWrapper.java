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
	private int order; // The address order in list
	private int weight; // The weight of this address
	/**
	 * Main memcached node address,if this is a main node,then this value is
	 * null.
	 */
	private InetSocketAddress mainNodeAddress;

	public InetSocketAddressWrapper(InetSocketAddress inetSocketAddress,
			int order, int weight, InetSocketAddress mainNodeAddress) {
		super();
		this.inetSocketAddress = inetSocketAddress;
		this.order = order;
		this.weight = weight;
		this.mainNodeAddress = mainNodeAddress;
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

	public int getWeight() {
		return this.weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public InetSocketAddress getMainNodeAddress() {
		return this.mainNodeAddress;
	}

	public void setMainNodeAddress(InetSocketAddress mainNodeAddress) {
		this.mainNodeAddress = mainNodeAddress;
	}

	public final void setOrder(int order) {
		this.order = order;
	}

}
