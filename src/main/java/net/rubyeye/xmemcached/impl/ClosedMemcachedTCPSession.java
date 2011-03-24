package net.rubyeye.xmemcached.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.CodecFactory.Decoder;
import com.google.code.yanf4j.core.CodecFactory.Encoder;

/**
 * Closed session
 * 
 * @author dennis
 * 
 */
public class ClosedMemcachedTCPSession implements MemcachedSession {
	private InetSocketAddressWrapper inetSocketAddressWrapper;
	private volatile boolean allowReconnect = true;

	public ClosedMemcachedTCPSession(
			InetSocketAddressWrapper inetSocketAddressWrapper) {
		super();
		this.inetSocketAddressWrapper = inetSocketAddressWrapper;
	}

	public void destroy() {

	}

	public InetSocketAddressWrapper getInetSocketAddressWrapper() {
		return this.inetSocketAddressWrapper;
	}

	public int getOrder() {
		return this.inetSocketAddressWrapper.getOrder();
	}

	public int getWeight() {
		return this.inetSocketAddressWrapper.getWeight();
	}

	public boolean isAllowReconnect() {
		return this.allowReconnect;
	}

	public void quit() {

	}

	public void setAllowReconnect(boolean allow) {
		this.allowReconnect = allow;
	}

	public void setBufferAllocator(BufferAllocator allocator) {

	}

	public void clearAttributes() {

	}

	public void close() {

	}

	public void flush() {

	}

	public Object getAttribute(String key) {

		return null;
	}

	public Decoder getDecoder() {

		return null;
	}

	public Encoder getEncoder() {

		return null;
	}

	public Handler getHandler() {

		return null;
	}

	public long getLastOperationTimeStamp() {

		return 0;
	}

	public InetAddress getLocalAddress() {

		return null;
	}

	public ByteOrder getReadBufferByteOrder() {

		return null;
	}

	public InetSocketAddress getRemoteSocketAddress() {
		return this.inetSocketAddressWrapper.getInetSocketAddress();
	}

	public long getScheduleWritenBytes() {

		return 0;
	}

	public long getSessionIdleTimeout() {

		return 0;
	}

	public long getSessionTimeout() {

		return 0;
	}

	public boolean isClosed() {
		return true;
	}

	public boolean isExpired() {

		return false;
	}

	public boolean isHandleReadWriteConcurrently() {
		return true;
	}

	public boolean isIdle() {

		return false;
	}

	public boolean isLoopbackConnection() {

		return false;
	}

	public boolean isUseBlockingRead() {

		return false;
	}

	public boolean isUseBlockingWrite() {

		return false;
	}

	public void removeAttribute(String key) {

	}

	public void setAttribute(String key, Object value) {

	}

	public Object setAttributeIfAbsent(String key, Object value) {

		return null;
	}

	public void setDecoder(Decoder decoder) {

	}

	public void setEncoder(Encoder encoder) {

	}

	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {

	}

	public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {

	}

	public void setSessionIdleTimeout(long sessionIdleTimeout) {

	}

	public void setSessionTimeout(long sessionTimeout) {

	}

	public void setUseBlockingRead(boolean useBlockingRead) {

	}

	public boolean isAuthFailed() {
		return false;
	}

	public void setAuthFailed(boolean authFailed) {

	}

	public void setUseBlockingWrite(boolean useBlockingWrite) {

	}

	public void start() {

	}

	public void write(Object packet) {

	}

}
