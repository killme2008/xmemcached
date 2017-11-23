package net.rubyeye.xmemcached.test.unittest;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

public class MockMemcachedSession extends MockSession
		implements
			MemcachedSession {

	public MockMemcachedSession(int port) {
		super(port);
	}

	public void setAllowReconnect(boolean allow) {
		// TODO Auto-generated method stub

	}

	public boolean isAllowReconnect() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setBufferAllocator(BufferAllocator allocator) {
		// TODO Auto-generated method stub

	}

	public InetSocketAddressWrapper getInetSocketAddressWrapper() {
		InetSocketAddressWrapper inetSocketAddressWrapper = new InetSocketAddressWrapper(
				getRemoteSocketAddress(), 1, 1, null);
		inetSocketAddressWrapper
				.setRemoteAddressStr("localhost/127.0.0.1:" + this.port);
		return inetSocketAddressWrapper;
	}

	public void destroy() {
		// TODO Auto-generated method stub

	}

	public int getWeight() {
		// TODO Auto-generated method stub
		return 1;
	}

	public int getOrder() {
		// TODO Auto-generated method stub
		return 0;
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

}
