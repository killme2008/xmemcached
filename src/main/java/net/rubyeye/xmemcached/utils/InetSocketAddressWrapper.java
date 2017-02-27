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
	private volatile String remoteAddressStr;
	private volatile String hostName;
	private volatile String mainNodeHostName;
	/**
	 * Main memcached node address,if this is a main node,then this value is
	 * null.
	 */
	private InetSocketAddress mainNodeAddress;

	public InetSocketAddressWrapper(InetSocketAddress inetSocketAddress,
			int order, int weight, InetSocketAddress mainNodeAddress) {
		super();
		setInetSocketAddress(inetSocketAddress);
		setMainNodeAddress(mainNodeAddress);
		this.order = order;
		this.weight = weight;
	}

	public String getRemoteAddressStr() {
		return this.remoteAddressStr;
	}

	public void setRemoteAddressStr(String remoteAddressStr) {
		this.remoteAddressStr = remoteAddressStr;
	}

	public final InetSocketAddress getInetSocketAddress() {
		if (ByteUtils.isValidString(this.hostName)) {
			// If it has a hostName, we try to resolve it again.
			return new InetSocketAddress(this.hostName,
					this.inetSocketAddress.getPort());
		} else {
			return this.inetSocketAddress;
		}
	}

	public final void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
		if (inetSocketAddress != null) {
			this.hostName = inetSocketAddress.getHostName();
		}
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
		if (ByteUtils.isValidString(this.mainNodeHostName)) {
			return new InetSocketAddress(this.mainNodeHostName,
					this.mainNodeAddress.getPort());
		} else {
			return this.mainNodeAddress;
		}
	}

	public void setMainNodeAddress(InetSocketAddress mainNodeAddress) {
		this.mainNodeAddress = mainNodeAddress;
		if (mainNodeAddress != null) {
			this.mainNodeHostName = mainNodeAddress.getHostName();
		}
	}

	public final void setOrder(int order) {
		this.order = order;
	}

}
