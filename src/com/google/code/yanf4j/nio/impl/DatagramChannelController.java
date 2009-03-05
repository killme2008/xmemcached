package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.util.Queue;

public abstract class DatagramChannelController extends AbstractController {
	protected DatagramChannel channel;
	protected Session udpSession;
	protected int maxDatagramPacketLength;

	public DatagramChannelController() {
		super();
		this.maxDatagramPacketLength = 4096;
	}

	protected void doStart() throws IOException {
		buildDatagramChannel();
		registerChannel();
		initializeReactor();
		buildUDPSession();
	}

	protected void registerChannel() throws ClosedChannelException {
		this.selectionKey = this.channel.register(this.selector,
				SelectionKey.OP_READ);
	}

	public DatagramChannelController(Configuration configuration) {
		super(configuration, null, null);
		setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
	}

	@SuppressWarnings("unchecked")
	public DatagramChannelController(Configuration configuration,
			CodecFactory codecFactory) {
		super(configuration, null, codecFactory);
		setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
	}

	@SuppressWarnings("unchecked")
	public DatagramChannelController(Configuration configuration,
			Handler handler, CodecFactory codecFactory) {
		super(configuration, handler, codecFactory);
		setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
	}

	public int getMaxDatagramPacketLength() {
		return maxDatagramPacketLength;
	}

	@Override
	public void setReadThreadCount(int readThreadCount) {
		if (readThreadCount > 1)
			throw new IllegalArgumentException(
					"UDP controller could not have more than 1 read thread");
		super.setReadThreadCount(readThreadCount);
	}

	public void setMaxDatagramPacketLength(int maxDatagramPacketLength) {
		if (started)
			throw new IllegalStateException();
		String osName = System.getProperties().getProperty("os.name");
		if (isLinux(osName) && maxDatagramPacketLength > 9216)
			throw new IllegalArgumentException(
					"The maxDatagramPacketLength could not be larger than 9216 bytes on linux");
		else if (maxDatagramPacketLength > 65507)
			throw new IllegalArgumentException(
					"The maxDatagramPacketLength could not be larger than 65507 bytes");
		this.maxDatagramPacketLength = maxDatagramPacketLength;
	}

	protected boolean isLinux(String osName) {
		return osName.indexOf("linux") >= 0 || osName.indexOf("Linux") >= 0;
	}

	public void closeChannel() throws IOException {
		if (this.udpSession != null && !this.udpSession.isClose())
			this.udpSession.close();
		if (channel != null)
			channel.close();
	}

	protected void buildUDPSession() {
		Queue<Session.WriteMessage> queue = buildQueue();
		udpSession = new DefaultUDPSession(selectionKey, channel, getReactor(),
				handler, this.maxDatagramPacketLength, statistics,
				getCodecFactory(), queue, handleReadWriteConcurrently);
		this.selectionKey.attach(udpSession);
		udpSession.start();
	}

	protected void buildDatagramChannel() throws IOException, SocketException,
			ClosedChannelException {
		this.channel = DatagramChannel.open();
		this.channel.socket().setSoTimeout(timeout);
		this.channel.socket().setReuseAddress(reuseAddress);
		if (this.receiveBufferSize > 0)
			this.channel.socket().setReceiveBufferSize(this.receiveBufferSize);
		this.channel.socket().bind(this.socketAddress);
		this.channel.configureBlocking(false);

	}

	public void onWrite(SelectionKey key) {
		this.udpSession.onEvent(EventType.WRITEABLE, selector);
	}

	@Override
	protected Runnable getReadHandler(SelectionKey key) {
		return new Runnable() {
			public void run() {
				udpSession.onEvent(EventType.READABLE, selector);
			}
		};
	}
}
