package net.rubyeye.xmemcached.impl;

import java.net.InetSocketAddress;

/**
 * Auto reconnect request
 * 
 * @author dennis
 * 
 */
public class ReconnectRequest {

	private InetSocketAddress address;
	private int tries;

	private int weight;

	public ReconnectRequest(InetSocketAddress address, int tries, int weight) {
		super();
		this.setAddress(address);
		this.setTries(tries); // record reconnect times
		this.weight = weight;
	}

	public final void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public final InetSocketAddress getAddress() {
		return address;
	}

	public final void setTries(int tries) {
		this.tries = tries;
	}

	public final int getTries() {
		return tries;
	}

	public final int getWeight() {
		return weight;
	}

	public final void setWeight(int weight) {
		this.weight = weight;
	}

}