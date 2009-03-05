package com.google.code.yanf4j.nio;

import java.net.DatagramPacket;
import java.net.SocketAddress;

public interface UDPSession extends Session {
	public boolean send(SocketAddress targetAddr, Object msg)
			throws InterruptedException;

	public boolean send(DatagramPacket packet) throws InterruptedException;
}
