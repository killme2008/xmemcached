package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Server address aware interface.Command which implement this interface have
 * these methods to getter/setter memcached's InetSocketAddress.
 * 
 * @author boyan
 * 
 */
public interface ServerAddressAware {
	public static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n"
			.getBytes());

	public InetSocketAddress getServer();

	public void setServer(InetSocketAddress server);

}
