package net.rubyeye.xmemcached.test.unittest.impl;

import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class KetamaMemcachedSessionLocatorNginxUpstreamConsistentUnitTest
		extends AbstractMemcachedSessionLocatorUnitTest {

	static private class MockSession implements Session {

		private boolean closed = false;
		private final String host;
		private final int port;

		public MockSession(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public void start() {
		}

		public void write(Object packet) {
		}

		public boolean isClosed() {
			return this.closed;
		}

		public void close() {
			this.closed = true;
		}

		public InetSocketAddress getRemoteSocketAddress() {
			return new InetSocketAddress(this.host, this.port);
		}

		public InetAddress getLocalAddress() {
			return null;
		}

		public boolean isUseBlockingWrite() {
			return false;
		}

		public void setUseBlockingWrite(boolean useBlockingWrite) {
		}

		public boolean isUseBlockingRead() {
			return false;
		}

		public void setUseBlockingRead(boolean useBlockingRead) {
		}

		public void flush() {
		}

		public boolean isExpired() {
			return false;
		}

		public boolean isIdle() {
			return false;
		}

		public CodecFactory.Encoder getEncoder() {
			return null;
		}

		public void setEncoder(CodecFactory.Encoder encoder) {
		}

		public CodecFactory.Decoder getDecoder() {
			return null;
		}

		public void setDecoder(CodecFactory.Decoder decoder) {
		}

		public boolean isHandleReadWriteConcurrently() {
			return false;
		}

		public void setHandleReadWriteConcurrently(
				boolean handleReadWriteConcurrently) {
		}

		public ByteOrder getReadBufferByteOrder() {
			return null;
		}

		public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
		}

		public void setAttribute(String key, Object value) {
		}

		public void removeAttribute(String key) {
		}

		public Object getAttribute(String key) {
			return null;
		}

		public void clearAttributes() {
		}

		public long getScheduleWritenBytes() {
			return 0;
		}

		public long getLastOperationTimeStamp() {
			return 0;
		}

		public boolean isLoopbackConnection() {
			return false;
		}

		public long getSessionIdleTimeout() {
			return 0;
		}

		public void setSessionIdleTimeout(long sessionIdleTimeout) {
		}

		public long getSessionTimeout() {
			return 0;
		}

		public void setSessionTimeout(long sessionTimeout) {
		}

		public Object setAttributeIfAbsent(String key, Object value) {
			return null;
		}

		public Handler getHandler() {
			return null;
		}
	}

	@Before
	public void setUp() {
		this.locator = new KetamaMemcachedSessionLocator();
	}

	@Test
	public void testSessionKey_CompatibleWithNginxUpstreamConsistent() {

		this.locator = new KetamaMemcachedSessionLocator(true);

		MockSession session1 = new MockSession("127.0.0.1", 11211);
		MockSession session2 = new MockSession("127.0.0.1", 11212);
		MockSession session3 = new MockSession("127.0.0.1", 11213);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertEquals(session1.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1-0").getRemoteSocketAddress());
		assertEquals(session1.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1-1").getRemoteSocketAddress());
		assertEquals(session1.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1-39").getRemoteSocketAddress());

		assertEquals(session2.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1:11212-0").getRemoteSocketAddress());
		assertEquals(session2.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1:11212-1").getRemoteSocketAddress());
		assertEquals(session2.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1:11212-39").getRemoteSocketAddress());

		assertEquals(session3.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1:11213-0").getRemoteSocketAddress());
		assertEquals(session3.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1:11213-1").getRemoteSocketAddress());
		assertEquals(session3.getRemoteSocketAddress(), this.locator
				.getSessionByKey("127.0.0.1:11213-39").getRemoteSocketAddress());

	}

	@Test
	public void testSessionKey_CompatibleWithNginxUpstreamConsistent_DefaultPort() {

		this.locator = new KetamaMemcachedSessionLocator(true);

		MockSession session1 = new MockSession("192.168.1.1", 11211);
		MockSession session2 = new MockSession("192.168.1.2", 11211);
		MockSession session3 = new MockSession("192.168.1.3", 11211);
		List<Session> list = new ArrayList<Session>();
		list.add(session1);
		list.add(session2);
		list.add(session3);
		this.locator.updateSessions(list);

		assertEquals(session1.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.1-0").getRemoteSocketAddress());
		assertEquals(session1.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.1-1").getRemoteSocketAddress());
		assertEquals(session1.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.1-39").getRemoteSocketAddress());

		assertEquals(session2.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.2-0").getRemoteSocketAddress());
		assertEquals(session2.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.2-1").getRemoteSocketAddress());
		assertEquals(session2.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.2-39").getRemoteSocketAddress());

		assertEquals(session3.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.3-0").getRemoteSocketAddress());
		assertEquals(session3.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.3-1").getRemoteSocketAddress());
		assertEquals(session3.getRemoteSocketAddress(), this.locator
				.getSessionByKey("192.168.1.3-39").getRemoteSocketAddress());

	}
}
