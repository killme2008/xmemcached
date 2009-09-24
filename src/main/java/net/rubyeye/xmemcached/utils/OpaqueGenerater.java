package net.rubyeye.xmemcached.utils;

/**
 * Opaque generator for memcached binary xxxq(getq,addq etc.) commands
 * 
 * @author dennis
 * 
 */
public final class OpaqueGenerater {
	private OpaqueGenerater() {

	}

	private int counter = 0;

	static final class SingletonHolder {
		static final OpaqueGenerater opaqueGenerater = new OpaqueGenerater();
	}

	public static OpaqueGenerater getInstance() {
		return SingletonHolder.opaqueGenerater;
	}

	public synchronized int getNextValue() {
		int result = this.counter++;
		if (result < 0) {
			this.counter = 0;
		}
		return result;
	}

}
