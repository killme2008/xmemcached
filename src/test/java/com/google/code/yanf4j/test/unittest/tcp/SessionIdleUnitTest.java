package com.google.code.yanf4j.test.unittest.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPConnectorController;
import com.google.code.yanf4j.nio.TCPController;

public class SessionIdleUnitTest extends TestCase {
	TCPController controller;

	final AtomicInteger serverIdleCount = new AtomicInteger(0);

	public void setUp() throws IOException {
		controller = new TCPController();
		controller.setSessionIdleTimeout(1000);
		controller.setHandler(new HandlerAdapter() {

			@Override
			public void onSessionIdle(Session session) {
				System.out.println("Session is idle");
				serverIdleCount.incrementAndGet();
			}

		});
		controller.bind(1999);
	}

	public void tearDown() throws IOException {
		if (this.controller != null)
			this.controller.stop();
		serverIdleCount.set(0);
	}

	public void testSessionIdle() throws Exception {
		TCPConnectorController connector = new TCPConnectorController();

		connector.setHandler(new HandlerAdapter() {

		});
		connector.connect(new InetSocketAddress("localhost", 1999));
		connector.awaitConnectUnInterrupt();
		synchronized (this) {
			while (serverIdleCount.get() < 5) {
				this.wait(1000);
			}
		}
		connector.stop();
	}

}
