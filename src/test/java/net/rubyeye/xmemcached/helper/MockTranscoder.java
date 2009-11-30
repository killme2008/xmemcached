package net.rubyeye.xmemcached.helper;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class MockTranscoder<T> implements Transcoder<T> {
	private volatile int count;
	private SerializingTranscoder serializingTranscoder = new SerializingTranscoder();



	public int getCount() {
		return count;
	}

	public T decode(CachedData d) {
		count++;
		return (T) serializingTranscoder.decode(d);
	}

	public CachedData encode(T o) {
		count++;
		return serializingTranscoder.encode(o);
	}

	public boolean isPackZeros() {
		return serializingTranscoder.isPackZeros();
	}

	public boolean isPrimitiveAsString() {
		return serializingTranscoder.isPrimitiveAsString();
	}

	public void setCompressionThreshold(int to) {
		serializingTranscoder.setCompressionThreshold(to);

	}

	public void setPackZeros(boolean packZeros) {
		serializingTranscoder.setPackZeros(packZeros);

	}

	public void setPrimitiveAsString(boolean primitiveAsString) {
		serializingTranscoder.setPrimitiveAsString(primitiveAsString);

	}

}
