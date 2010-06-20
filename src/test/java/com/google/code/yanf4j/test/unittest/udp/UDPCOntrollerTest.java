package com.google.code.yanf4j.test.unittest.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.junit.Ignore;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.UDPSession;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.core.impl.TextLineCodecFactory;
import com.google.code.yanf4j.core.impl.UDPHandlerAdapter;
import com.google.code.yanf4j.nio.UDPConnectorController;
import com.google.code.yanf4j.nio.UDPController;

@Ignore
public class UDPCOntrollerTest extends TestCase {
	private static final int PORT = 11932;
	private static final InetSocketAddress INET_SOCKET_ADDRESS = new InetSocketAddress(
			"localhost", PORT);
	private static final int SENT_COUNT = 10;
	UDPController controller;
	AtomicInteger serverReceived, clientReceived, serverSent;

	@Override
	public void setUp() throws IOException {
		this.controller = new UDPController();
		this.controller.setCodecFactory(new TextLineCodecFactory());
		this.controller.setHandler(new UDPHandlerAdapter() {

			@Override
			public void onMessageReceived(UDPSession udpSession, SocketAddress address,
					Object message) {
				System.out.println(address);
				UDPCOntrollerTest.this.serverReceived.incrementAndGet();
				try {
					assertTrue(udpSession.asyncWrite(address, message).get());
				} catch (Exception e) {
					fail();
				}
			}

			@Override
			public void onMessageSent(Session session, Object msg) {
				UDPCOntrollerTest.this.serverSent.incrementAndGet();
			}

		});

		this.controller.bind(INET_SOCKET_ADDRESS);
		assertTrue(this.controller.isStarted());
		this.serverReceived = new AtomicInteger();
		this.serverSent = new AtomicInteger();
		this.clientReceived = new AtomicInteger();
	}

	public void testEcho() throws Exception {
		UDPConnectorController connector = new UDPConnectorController();
		connector.setCodecFactory(new TextLineCodecFactory());
		connector.setHandler(new HandlerAdapter() {
			@Override
			public void onMessageReceived(Session session, Object msg) {
				if (UDPCOntrollerTest.this.clientReceived.incrementAndGet() == SENT_COUNT) {
					synchronized (UDPCOntrollerTest.this) {
						UDPCOntrollerTest.this.notifyAll();
					}
				}

			}
		});
		connector.start();
		assertTrue(connector.isStarted());
		connector.connect(INET_SOCKET_ADDRESS);
		for (int i = 0; i < SENT_COUNT; i++) {
			assertTrue(connector.send(
					new DatagramPacket("hello\r\n".getBytes(), 7)).get());
		}
		synchronized (this) {
			while (this.clientReceived.get() != SENT_COUNT) {
				this.wait(1000);
			}
		}
		assertEquals(SENT_COUNT, this.serverReceived.get());
		assertEquals(SENT_COUNT, this.serverSent.get());
		assertEquals(SENT_COUNT, this.clientReceived.get());
		// test disconnect
		connector.disconnect();
		try {
			connector.send(new DatagramPacket("hello\r\n".getBytes(), 7));
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Controller has been stopped", e.getMessage());
		}
		connector.connect(INET_SOCKET_ADDRESS);
		assertTrue(connector
				.send(new DatagramPacket("hello\r\n".getBytes(), 7)).get());
		Thread.sleep(1000);
		assertEquals(SENT_COUNT + 1, this.serverReceived.get());
		connector.stop();
		assertFalse(connector.isStarted());
	}

	@Override
	public void tearDown() throws IOException {
		this.controller.stop();
		assertTrue(!this.controller.isStarted());
	}
}
