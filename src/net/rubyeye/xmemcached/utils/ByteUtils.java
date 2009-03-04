package net.rubyeye.xmemcached.utils;

import java.io.UnsupportedEncodingException;

public class ByteUtils {

	public static byte[] getBytes(String k) {
		try {
			return k.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
