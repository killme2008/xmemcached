package com.google.code.yanf4j.test.unittest.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPConnectorController;
import com.google.code.yanf4j.nio.TCPController;

public class SessionTimeoutUnitTest extends TestCase{

	TCPController controller;

	final AtomicBoolean expired = new AtomicBoolean(false);

	public void setUp() throws IOException {
		controller = new TCPController();
		controller.setSessionTimeout(2000);
		controller.setHandler(new HandlerAdapter() {

			@Override
			public void onSessionExpired(Session session) {
				System.out.println("Server End,session is expired");
				expired.set(true);
			}

		});
		controller.bind(1997);
	}

	public void tearDown() throws IOException {
		if (this.controller != null)
			this.controller.stop();
		expired.set(false);
	}

	public void testSessionTimeout() throws Exception {
		TCPConnectorController connector = new TCPConnectorController();
        final AtomicBoolean closed=new AtomicBoolean(false);
		connector.setHandler(new HandlerAdapter() {

			@Override
			public void onSessionClosed(Session session) {
				System.out.println("Client End,session is closed");
				closed.set(true);
			}

		});
		connector.connect(new InetSocketAddress("localhost", 1997));
		connector.awaitConnectUnInterrupt();
		synchronized (this) {
			while (!expired.get()||!closed.get()) {
				this.wait(1000);
			}
		}
	}

}
