package net.rubyeye.xmemcached.uds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;

import com.google.code.juds.UnixDomainSocketClient;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.CodecFactory.Decoder;
import com.google.code.yanf4j.core.CodecFactory.Encoder;

public class UDSocketSession implements Session {
	private UnixDomainSocketClient socketClient;
	private InputStream in;
	private DataOutputStream out;
	private volatile boolean closed;
	private final ByteBuffer readBuffer = ByteBuffer.allocate(512 * 1024);
	private final UDSocketAddress remotingAddress;
	private final BufferAllocator allocator;

	private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap();

	public UDSocketSession(String path, UnixDomainSocketClient client,
			BufferAllocator bufferAllocator) {
		this.socketClient = client;
		this.in = this.socketClient.getInputStream();
		this.out = new DataOutputStream(this.socketClient.getOutputStream());
		this.remotingAddress = new UDSocketAddress(path);
		this.allocator = bufferAllocator;
	}

	public Future<Boolean> asyncWrite(Object packet) {
		throw new UnsupportedOperationException();
	}

	public void clearAttributes() {
		attributes.clear();

	}

	public void close() {
		if (this.closed)
			return;
		closed = true;
		this.socketClient.close();

	}

	public void flush() {
		try {
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
	}

	public Object getAttribute(String key) {
		return this.attributes.get(key);
	}

	public Decoder getDecoder() {
		return null;
	}

	public Encoder getEncoder() {
		return null;
	}

	public long getLastOperationTimeStamp() {
		return 0;
	}

	public ByteOrder getReadBufferByteOrder() {
		return this.readBuffer.order();
	}

	public InetSocketAddress getRemoteSocketAddress() {
		return this.remotingAddress;
	}

	public long getScheduleWritenBytes() {
		return 0;
	}

	public long getSessionIdleTimeout() {
		return 0;
	}

	public long getSessionTimeout() {
		return this.socketClient == null ? 0 : this.socketClient.getTimeout();
	}

	public boolean isClosed() {
		return this.closed;
	}

	public boolean isExpired() {
		return false;
	}

	public boolean isHandleReadWriteConcurrently() {

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
		this.attributes.remove(key);
	}

	public void setAttribute(String key, Object value) {
		this.attributes.put(key, value);

	}

	public Object setAttributeIfAbsent(String key, Object value) {
		return this.attributes.putIfAbsent(key, value);
	}

	public void setDecoder(Decoder decoder) {

	}

	public void setEncoder(Encoder encoder) {

	}

	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {

	}

	public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
		this.readBuffer.order(readBufferByteOrder);

	}

	public void setSessionIdleTimeout(long sessionIdleTimeout) {

	}

	public void setSessionTimeout(long sessionTimeout) {

	}

	public void setUseBlockingRead(boolean useBlockingRead) {

	}

	public void setUseBlockingWrite(boolean useBlockingWrite) {

	}

	public void start() {

	}

	final byte[] buffer = new byte[4096];

	public void readFromInputStream(Command command) throws IOException {
		while (true) {
			int readCount = in.read(buffer);
			if (readCount > 0) {
				this.readBuffer.put(buffer, 0, readCount);
				this.readBuffer.flip();
				if (command.decode(null, this.readBuffer))
					break;
				this.readBuffer.compact();
			}
		}
		this.readBuffer.clear();

	}

	public void write(Object packet) {
		Command command = (Command) packet;
		command.encode(this.allocator);
		ByteBuffer buffer = command.getIoBuffer().getByteBuffer();
		final byte[] data = buffer.array();
		try {
			out.write(data, 0, data.length);
		} catch (IOException e) {
			throw new RuntimeException("Write command error", e);
		}
	}

}
