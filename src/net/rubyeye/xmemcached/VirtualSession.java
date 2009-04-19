/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rubyeye.xmemcached;

import com.google.code.yanf4j.nio.CodecFactory.Decoder;
import com.google.code.yanf4j.nio.CodecFactory.Encoder;
import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.util.EventType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Map;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.utils.LRUMap;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

/**
 * virtual node
 *
 * @author dennis
 */
public class VirtualSession implements Session {

	private int maxSize;
	private final Map<String, CachedData> cache;
	private volatile boolean closed;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;

	@SuppressWarnings("unchecked")
	public VirtualSession(int maxSize, Transcoder transcoder) {
		this.maxSize = maxSize;
		closed = false;
		this.cache = Collections
				.synchronizedMap(new LRUMap<String, CachedData>(this.maxSize));
		this.transcoder = transcoder;
	}

	@Override
	public void attach(Object obj) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object attachment() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void close() {
		if (isClosed()) {
			return;
		}
		this.closed = true;
		this.cache.clear();
	}

	@Override
	public void flush() throws IOException, InterruptedException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	@SuppressWarnings("unchecked")
	public Decoder getDecoder() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	@SuppressWarnings("unchecked")
	public Encoder getEncoder() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ByteOrder getReadBufferByteOrder() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {

		throw new UnsupportedOperationException("Not supported yet.");
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
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isUseBlockingRead() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isUseBlockingWrite() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onEvent(EventType event, Selector selector) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean send(Object packet) {
		Command command = (Command) packet;
		String key = (String) command.getKey();
		switch (command.getCommandType()) {
		case GET_ONE:
			command.setResult(this.cache.get(key));
			command.getLatch().countDown();
			break;
		case SET:
			this.cache.put(key, command.getStoredData());
			command.setResult(Boolean.TRUE);
			command.getLatch().countDown();
			break;
		case ADD:
			synchronized (cache) {
				if (cache.get(key) == null) {
					this.cache.put(key, command.getStoredData());
					command.setResult(Boolean.TRUE);
				} else {
					command.setResult(Boolean.FALSE);
				}
			}
			command.getLatch().countDown();
			break;
		case REPLACE:
			synchronized (cache) {
				if (cache.get(key) != null) {
					this.cache.put(key, command.getStoredData());
					command.setResult(Boolean.TRUE);
				} else {
					command.setResult(Boolean.FALSE);
				}
			}
			command.getLatch().countDown();
			break;

		case APPEND:
			synchronized (cache) {
				if (cache.get(key) != null) {
					CachedData existsData = this.cache.get(key);
					byte[] oldData = existsData.getData();
					byte[] newData = command.getStoredData().getData();
					byte[] data = new byte[oldData.length + newData.length];
					System.arraycopy(oldData, 0, data, 0, oldData.length);
					System.arraycopy(newData, 0, data, oldData.length,
							newData.length);
					cache.put(key, new CachedData(existsData.getFlags(), data,
							data.length, -1));
					command.setResult(Boolean.TRUE);
				} else {
					command.setResult(Boolean.FALSE);
				}
			}
			command.getLatch().countDown();
			break;
		case PREPEND:
			synchronized (cache) {
				if (cache.get(key) != null) {
					CachedData existsData = this.cache.get(key);
					byte[] oldData = existsData.getData();
					byte[] newData = command.getStoredData().getData();
					byte[] data = new byte[oldData.length + newData.length];
					System.arraycopy(newData, 0, data, 0, newData.length);
					System.arraycopy(oldData, 0, data, newData.length,
							oldData.length);
					cache.put(key, new CachedData(existsData.getFlags(), data,
							data.length, -1));
					command.setResult(Boolean.TRUE);
				} else {
					command.setResult(Boolean.FALSE);
				}
			}
			command.getLatch().countDown();
			break;
		case DECR:
		case INCR:
			synchronized (cache) {
				if (cache.get(key) != null) {
					CachedData existsData = this.cache.get(key);
					Integer count = Integer.parseInt((String) this.transcoder
							.decode(existsData));
					count = command.getCommandType() == Command.CommandType.INCR ? count + 1
							: count - 1;
					CachedData newData = this.transcoder.encode(String
							.valueOf(count));
					cache.put(key, newData);
					command.setResult(newData);
				} else {
					CachedData newData = this.transcoder.encode("0");
					cache.put(key, newData);
					command.setResult(newData);

				}
			}
			command.getLatch().countDown();
			break;
		case DELETE:
			synchronized (cache) {
				if (cache.get(key) != null) {
					cache.remove(key);
					command.setResult(Boolean.TRUE);
				} else {
					command.setResult(Boolean.FALSE);
				}
			}
			command.getLatch().countDown();
			break;
		default:
			throw new UnsupportedOperationException(
					"Unsupported operation for virtual session:"
							+ command.getCommandType().name());
		}
		return true;
	}

	@Override
	public boolean send(Object msg, long timeout) throws InterruptedException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setDecoder(Decoder decoder) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setEncoder(Encoder encoder) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setUseBlockingRead(boolean useBlockingRead) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setUseBlockingWrite(boolean useBlockingWrite) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void start() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public long transferFrom(long position, long count, FileChannel source)
			throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public long transferTo(long position, long count, FileChannel target)
			throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
