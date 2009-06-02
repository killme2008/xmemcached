package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.CodecFactory.Decoder;
import com.google.code.yanf4j.nio.CodecFactory.Encoder;
import com.google.code.yanf4j.nio.util.EventType;

public class MockSession implements Session {
	private boolean closed = false;
	private int port;

	public MockSession(int port) {
		this.port = port;
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
	public void flush() throws IOException, InterruptedException {

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
		return new InetSocketAddress("localhost", port);
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
	public void onEvent(EventType event, Selector selector) {

	}

	@Override
	public boolean send(Object msg, long timeout) throws InterruptedException {

		return false;
	}

	@Override
	public boolean send(Object packet) {

		return false;
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
	public long transferFrom(long position, long count, FileChannel source)
			throws IOException {

		return 0;
	}

	@Override
	public long transferTo(long position, long count, FileChannel target)
			throws IOException {

		return 0;
	}




	@Override
	public long getScheduleWritenBytes() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	

}
