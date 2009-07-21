package net.rubyeye.xmemcached.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface ServerAddressAware {
	public static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n"
			.getBytes());

	public InetSocketAddress getServer();

	public void setServer(InetSocketAddress server);

}
