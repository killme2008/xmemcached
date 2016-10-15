package net.rubyeye.xmemcached.aws;

import java.io.Serializable;

/**
 * AWS ElasticCache Node information.
 * 
 * @author dennis
 *
 */
public class CacheNode implements Serializable {

	private static final long serialVersionUID = -2999058612548153786L;

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
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
