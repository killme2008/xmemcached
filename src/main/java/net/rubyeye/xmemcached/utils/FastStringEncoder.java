package net.rubyeye.xmemcached.utils;

import java.util.Arrays;

/**
 * Fast string utf encoder.
 * 
 * @author dennis
 * 
 */
public class FastStringEncoder {

  private static final int STEP = 128;
  private static ThreadLocal<byte[]> bufLocal = new ThreadLocal<byte[]>();

  private static byte[] getBuf(int length) {
    byte[] buf = bufLocal.get();
    if (buf != null) {
      bufLocal.set(null);
    } else {
      buf = new byte[length < STEP ? STEP : length];
    }
    return buf;
  }

  private static void close(byte[] buf) {
    if (buf.length <= 1024 * 64) {
      bufLocal.set(buf);
    }
  }

  private static byte[] expandCapacity(byte[] buf, int minNewCapacity) {
    int newCapacity = buf.length + (buf.length >> 1) + 1;
    if (newCapacity < minNewCapacity) {
      newCapacity = minNewCapacity;
    }
    return Arrays.copyOf(buf, newCapacity);
  }

  public static byte[] encodeUTF8(String s) {
    int len = s.length();
    byte[] bytes = getBuf(len);

    int offset = 0;
    int sl = offset + len;
    int dp = 0;
    int dlASCII = dp + Math.min(len, bytes.length);

    // ASCII only optimized loop
    while (dp < dlASCII && s.charAt(offset) < '\u0080') {
      bytes[dp++] = (byte) s.charAt(offset++);
    }

    while (offset < sl) {
      if (dp >= bytes.length - 4) {
        bytes = expandCapacity(bytes, bytes.length + STEP * 2);
      }
      char c = s.charAt(offset++);
      if (c < 0x80) {
        // Have at most seven bits
        bytes[dp++] = (byte) c;
      } else if (c < 0x800) {
        // 2 bytes, 11 bits
        bytes[dp++] = (byte) (0xc0 | c >> 6);
        bytes[dp++] = (byte) (0x80 | c & 0x3f);
      } else if (c >= '\uD800' && c < '\uDFFF' + 1) { // Character.isSurrogate(c)
        // but 1.7
        final int uc;
        int ip = offset - 1;
        if (Character.isHighSurrogate(c)) {
          if (sl - ip < 2) {
            uc = -1;
          } else {
            char d = s.charAt(ip + 1);
            if (Character.isLowSurrogate(d)) {
              uc = Character.toCodePoint(c, d);
            } else {
              throw new IllegalStateException("encodeUTF8 error");
            }
          }
        } else {
          if (Character.isLowSurrogate(c)) {
            throw new IllegalStateException("encodeUTF8 error");
          } else {
            uc = c;
          }
        }

        if (uc < 0) {
          bytes[dp++] = (byte) '?';
        } else {
          bytes[dp++] = (byte) (0xf0 | uc >> 18);
          bytes[dp++] = (byte) (0x80 | uc >> 12 & 0x3f);
          bytes[dp++] = (byte) (0x80 | uc >> 6 & 0x3f);
          bytes[dp++] = (byte) (0x80 | uc & 0x3f);
          offset++; // 2 chars
        }
      } else {
        // 3 bytes, 16 bits
        bytes[dp++] = (byte) (0xe0 | c >> 12);
        bytes[dp++] = (byte) (0x80 | c >> 6 & 0x3f);
        bytes[dp++] = (byte) (0x80 | c & 0x3f);
      }
    }
    byte[] resultBytes = new byte[dp];
    System.arraycopy(bytes, 0, resultBytes, 0, dp);
    close(bytes);
    return resultBytes;
  }
}
