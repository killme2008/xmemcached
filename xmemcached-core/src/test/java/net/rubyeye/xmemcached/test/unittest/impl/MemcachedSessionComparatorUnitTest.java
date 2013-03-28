package net.rubyeye.xmemcached.test.unittest.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.impl.MemcachedSessionComparator;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.CodecFactory.Decoder;
import com.google.code.yanf4j.core.CodecFactory.Encoder;

public class MemcachedSessionComparatorUnitTest extends TestCase {
	static private class MockSession implements MemcachedSession, Session {
		private final int order;

		public InetSocketAddressWrapper getInetSocketAddressWrapper() {
			return new InetSocketAddressWrapper(null, this.order, 0, null);
		}

		public MockSession(int order) {
			super();
			this.order = order;
		}

		public void quit() {
			// TODO Auto-generated method stub

		}

		public boolean isAuthFailed() {
			// TODO Auto-generated method stub
			return false;
		}

		public void setAuthFailed(boolean authFailed) {
			// TODO Auto-generated method stub
			
		}

		public Future<Boolean> asyncWrite(Object packet) {
			// TODO Auto-generated method stub
			return null;
		}

		public void clearAttributes() {
			// TODO Auto-generated method stub

		}

		public void destroy() {
			// TODO Auto-generated method stub

		}

		public void close() {
			// TODO Auto-generated method stub

		}

		public void flush() {
			// TODO Auto-generated method stub

		}

		public Object getAttribute(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		public Decoder getDecoder() {
			// TODO Auto-generated method stub
			return null;
		}

		public Encoder getEncoder() {
			// TODO Auto-generated method stub
			return null;
		}

		public Handler getHandler() {
			// TODO Auto-generated method stub
			return null;
		}

		public long getLastOperationTimeStamp() {
			// TODO Auto-generated method stub
			return 0;
		}

		public InetAddress getLocalAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		public ByteOrder getReadBufferByteOrder() {
			// TODO Auto-generated method stub
			return null;
		}

		public InetSocketAddress getRemoteSocketAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		public long getScheduleWritenBytes() {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getSessionIdleTimeout() {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getSessionTimeout() {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean isClosed() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isExpired() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isHandleReadWriteConcurrently() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isIdle() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isLoopbackConnection() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isUseBlockingRead() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isUseBlockingWrite() {
			// TODO Auto-generated method stub
			return false;
		}

		public void removeAttribute(String key) {
			// TODO Auto-generated method stub

		}

		public void setAttribute(String key, Object value) {
			// TODO Auto-generated method stub

		}

		public Object setAttributeIfAbsent(String key, Object value) {
			// TODO Auto-generated method stub
			return null;
		}

		public void setDecoder(Decoder decoder) {
			// TODO Auto-generated method stub

		}

		public void setEncoder(Encoder encoder) {
			// TODO Auto-generated method stub

		}

		public void setHandleReadWriteConcurrently(
				boolean handleReadWriteConcurrently) {
			// TODO Auto-generated method stub

		}

		public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
			// TODO Auto-generated method stub

		}

		public void setSessionIdleTimeout(long sessionIdleTimeout) {
			// TODO Auto-generated method stub

		}

		public void setSessionTimeout(long sessionTimeout) {
			// TODO Auto-generated method stub

		}

		public void setUseBlockingRead(boolean useBlockingRead) {
			// TODO Auto-generated method stub

		}

		public void setUseBlockingWrite(boolean useBlockingWrite) {
			// TODO Auto-generated method stub

		}

		public void start() {
			// TODO Auto-generated method stub

		}

		public void write(Object packet) {
			// TODO Auto-generated method stub

		}

		public int getOrder() {
			return this.order;
		}

		public int getWeight() {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean isAllowReconnect() {
			// TODO Auto-generated method stub
			return false;
		}

		public void setAllowReconnect(boolean allow) {
			// TODO Auto-generated method stub

		}

		public void setBufferAllocator(BufferAllocator allocator) {
			// TODO Auto-generated method stub

		}

	}

	public void testCompare() {

		List<Session> sessionList = new ArrayList<Session>();
		for (int i = 0; i < 100; i++) {
			sessionList.add(new MockSession(i));
		}
		Collections.sort(sessionList, new MemcachedSessionComparator());

		for (int i = 0; i < sessionList.size(); i++) {
			if (i < sessionList.size() - 1) {
				int next = i + 1;
				MockSession nextSession = (MockSession) sessionList.get(next);
				MockSession currentSession = (MockSession) sessionList.get(i);
				assertTrue(currentSession.getOrder() < nextSession.getOrder());
			}
		}

	}
}
