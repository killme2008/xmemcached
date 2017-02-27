package net.rubyeye.xmemcached.aws;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * AWS ElasticCache Node information.
 * 
 * @author dennis
 *
 */
public class CacheNode implements Serializable {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheNode other = (CacheNode) obj;
		if (hostName == null) {
			if (other.hostName != null)
				return false;
		} else if (!hostName.equals(other.hostName))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	private static final long serialVersionUID = -2999058612548153786L;

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public InetSocketAddress getInetSocketAddress() {
		return new InetSocketAddress(hostName, port);
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String toString() {
		return "[" + this.hostName + "|" + this.ipAddress + "|" + this.port
				+ "]";
	}

	public String getCacheKey() {
		return this.hostName + ":" + this.port;
	}

	private String hostName;
	private String ipAddress;
	private int port;

	public CacheNode(String hostName, String ipAddress, int port) {
		super();
		this.hostName = hostName;
		this.ipAddress = ipAddress;
		this.port = port;
	}

}
