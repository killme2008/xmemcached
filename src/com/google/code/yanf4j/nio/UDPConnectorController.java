package com.google.code.yanf4j.nio;

import java.net.SocketAddress;
import java.io.IOException;
import java.net.DatagramPacket;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.impl.DatagramChannelController;
import com.google.code.yanf4j.nio.impl.DefaultUDPSession;

public class UDPConnectorController extends DatagramChannelController {

	private SocketAddress remoteAddress;

	public void connect(SocketAddress remoteAddress) throws IOException {
		this.remoteAddress = remoteAddress;
		this.start();
		this.channel.connect(remoteAddress);
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void disconnect() throws IOException {
		this.channel.disconnect();
	}

	public boolean send(DatagramPacket packet) throws InterruptedException {
		if (this.remoteAddress != null)
			packet.setSocketAddress(this.remoteAddress);
		return ((DefaultUDPSession) this.udpSession).send(packet);
	}

	public boolean send(SocketAddress targetAddr, Object msg)
			throws InterruptedException {
		return ((DefaultUDPSession) this.udpSession).send(targetAddr, msg);
	}

	public UDPConnectorController(Configuration configuration) {
		super(configuration, null, null);
		setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
	}

	public UDPConnectorController() {
		super();
	}

	@SuppressWarnings("unchecked")
	public UDPConnectorController(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
		setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
	}

	@SuppressWarnings("unchecked")
	public UDPConnectorController(Configuration configuration, Handler handler,
			CodecFactory codecFactory) {
		super(configuration, handler, codecFactory);
		setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
	}
}
