package net.rubyeye.xmemcached.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class ByteUtils {

    public static final byte[] CRLF = {'\r', '\n'};
    public static final byte[] GET = {'g', 'e', 't'};
    public static final byte[] DELETE = {'d', 'e', 'l', 'e', 't', 'e'};
    public static final byte SPACE = ' ';

    public static byte[] getBytes(String k) {
        try {
            return k.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static final void setArguments(ByteBuffer bb, Object... args) {
        boolean wasFirst = true;
        for (Object o : args) {
            if (wasFirst) {
                wasFirst = false;
            } else {
                bb.put(SPACE);
            }
            if (o instanceof byte[]) {
                bb.put((byte[]) o);
            } else {
                bb.put(ByteUtils.getBytes(String.valueOf(o)));
            }
        }
        bb.put(CRLF);
    }
}
