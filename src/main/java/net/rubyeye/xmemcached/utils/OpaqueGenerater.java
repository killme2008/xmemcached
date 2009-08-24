package net.rubyeye.xmemcached.utils;

public final class OpaqueGenerater {
	private OpaqueGenerater() {

	}

	private int counter = Integer.MIN_VALUE;

	static final class SingletonHolder {
		static final OpaqueGenerater opaqueGenerater = new OpaqueGenerater();
	}

	public static OpaqueGenerater getInstance() {
		return SingletonHolder.opaqueGenerater;
	}

	public synchronized int getNextValue() {
		int result = this.counter + 1;
		if (result < 0) {
			this.counter = Integer.MIN_VALUE;
		}
		return result;
	}

}
