package com.google.code.yanf4j.test.unittest.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPConnectorController;
import com.google.code.yanf4j.nio.TCPController;

/**
 * 流量控制测试
 * 
 * @author boyan
 * 
 */
public class SessionFlowControllUnitTest extends TestCase {

	TCPController controller;

	private AtomicInteger writeOverFlowTimes = new AtomicInteger();

	private AtomicLong totalServerWritten = new AtomicLong();

	private AtomicLong totalDiscardWritten = new AtomicLong();

	@Override
	public void setUp() throws IOException {
		Configuration conf = new Configuration();
		conf.setStatisticsServer(true);
		this.controller = new TCPController(conf);
		this.controller.setSendThroughputLimit(100);
		this.controller.setHandler(new HandlerAdapter() {

			@Override
			public void onMessageReceived(Session session, Object message) {
				session.write(((IoBuffer) message).duplicate());
				SessionFlowControllUnitTest.this.totalServerWritten.addAndGet(((IoBuffer) message).capacity());
			}

			@Override
			public boolean onSessionWriteOverFlow(Session session,
					Object message) {
				SessionFlowControllUnitTest.this.totalDiscardWritten
						.addAndGet(((IoBuffer) message).capacity());
				SessionFlowControllUnitTest.this.writeOverFlowTimes.incrementAndGet();
				// cancel sending
				return false;
			}

		});
		this.controller.bind(1998);
	}

	@Override
	public void tearDown() throws IOException {
		if (this.controller != null) {
			this.controller.stop();
		}
	}

	public void testWriteFlowControll() throws Exception {
		this.controller.setSendThroughputLimit(100);
		Configuration conf = new Configuration();
		conf.setStatisticsServer(true);
		TCPConnectorController connector = new TCPConnectorController(conf);
		final AtomicLong totalClientReceived = new AtomicLong();
		connector.setHandler(new HandlerAdapter() {

			@Override
			public void onMessageReceived(Session session, Object message) {
				totalClientReceived
						.addAndGet(((IoBuffer) message).capacity());
			}

		});

		connector.connect(new InetSocketAddress("localhost", 1998));
		connector.awaitConnectUnInterrupt();
		byte[] msg = new byte[100];
		for (int i = 0; i < 10000; i++) {
			connector.send(IoBuffer.wrap(msg));
		}
		synchronized (this) {
			while (this.writeOverFlowTimes.get() < 1
					|| totalClientReceived.get() == 0) {
				this.wait(1000);
			}
		}
		connector.stop();
		System.out.println("Client received:" + totalClientReceived
				+ ",server written really:"
				+ (this.totalServerWritten.get() - this.totalDiscardWritten.get())
				+ ",server discarded:" + this.totalDiscardWritten.get());
		Assert.assertEquals(totalClientReceived.get(), this.totalServerWritten.get()
				- this.totalDiscardWritten.get(), 500);

	}

	@Ignore
	public void testReceiveFlowControll() {
		

	}
}
