package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Future;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.CodecFactory.Decoder;
import com.google.code.yanf4j.core.CodecFactory.Encoder;
import com.google.code.yanf4j.core.impl.FutureImpl;

public class MockSession implements Session {
	private boolean closed = false;
	private final int port;

	public MockSession(int port) {
		this.port = port;
	}

	@Override
	public void write(Object packet) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearAttributes() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getAttribute(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAttribute(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAttribute(String key, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		this.closed = true;

	}

	@Override
	public void flush() {

	}

	@Override
	public Decoder getDecoder() {

		return null;
	}

	@Override
	public Encoder getEncoder() {

		return null;
	}

	@Override
	public ByteOrder getReadBufferByteOrder() {

		return null;
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return new InetSocketAddress("localhost", this.port);
	}

	@Override
	public boolean isClosed() {

		return this.closed;
	}

	@Override
	public boolean isExpired() {

		return false;
	}

	@Override
	public boolean isHandleReadWriteConcurrently() {

		return false;
	}

	@Override
	public boolean isUseBlockingRead() {

		return false;
	}

	@Override
	public boolean isUseBlockingWrite() {

		return false;
	}

	@Override
	public Future<Boolean> asyncWrite(Object packet) {

		return new FutureImpl<Boolean>();
	}

	@Override
	public void setDecoder(Decoder decoder) {

	}

	@Override
	public void setEncoder(Encoder encoder) {

	}

	@Override
	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {

	}

	@Override
	public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {

	}

	@Override
	public void setUseBlockingRead(boolean useBlockingRead) {

	}

	@Override
	public void setUseBlockingWrite(boolean useBlockingWrite) {

	}

	@Override
	public void start() {

	}

	@Override
	public long getScheduleWritenBytes() {
		return 0;
	}

	public void updateTimeStamp() {
	}

	public long getLastOperationTimeStamp() {
		return 0;
	}

	public final boolean isLoopbackConnection() {
		return false;
	}

}
