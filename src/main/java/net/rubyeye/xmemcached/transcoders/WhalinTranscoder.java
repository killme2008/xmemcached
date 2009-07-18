package net.rubyeye.xmemcached.transcoders;

import java.util.Date;

/**
 * Transcoder that provides compatibility with Greg Whalin's memcached client.
 */
public class WhalinTranscoder extends BaseSerializingTranscoder implements
		Transcoder<Object> {

	public static final int SPECIAL_BYTE = 1;
	public static final int SPECIAL_BOOLEAN = 8192;
	public static final int SPECIAL_INT = 4;
	public static final int SPECIAL_LONG = 16384;
	public static final int SPECIAL_CHARACTER = 16;
	public static final int SPECIAL_STRING = 32;
	public static final int SPECIAL_STRINGBUFFER = 64;
	public static final int SPECIAL_FLOAT = 128;
	public static final int SPECIAL_SHORT = 256;
	public static final int SPECIAL_DOUBLE = 512;
	public static final int SPECIAL_DATE = 1024;
	public static final int SPECIAL_STRINGBUILDER = 2048;
	public static final int SPECIAL_BYTEARRAY = 4096;
	public static final int COMPRESSED = 2;
	public static final int SERIALIZED = 8;

	private int maxSize;

	public WhalinTranscoder() {
		this(CachedData.MAX_SIZE);
	}

	public WhalinTranscoder(int maxSize) {
		super();
		this.maxSize = maxSize;
	}

	public final int getMaxSize() {
		return maxSize;
	}

	public final void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	private final TranscoderUtils tu = new TranscoderUtils(false);

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.spy.memcached.Transcoder#decode(net.spy.memcached.CachedData)
	 */
	public Object decode(CachedData d) {
		byte[] data = d.getData();
		Object rv = null;
		if ((d.getFlag() & COMPRESSED) != 0) {
			data = decompress(d.getData());
		}
		if ((d.getFlag() & SERIALIZED) != 0) {
			rv = deserialize(data);
		} else {
			int f = d.getFlag() & ~COMPRESSED;
			switch (f) {
			case SPECIAL_BOOLEAN:
				rv = Boolean.valueOf(this.decodeBoolean(data));
				break;
			case SPECIAL_INT:
				rv = Integer.valueOf(tu.decodeInt(data));
				break;
			case SPECIAL_SHORT:
				rv = Short.valueOf((short) tu.decodeInt(data));
				break;
			case SPECIAL_LONG:
				rv = Long.valueOf(tu.decodeLong(data));
				break;
			case SPECIAL_DATE:
				rv = new Date(tu.decodeLong(data));
				break;
			case SPECIAL_BYTE:
				rv = Byte.valueOf(tu.decodeByte(data));
				break;
			case SPECIAL_FLOAT:
				rv = new Float(Float.intBitsToFloat(tu.decodeInt(data)));
				break;
			case SPECIAL_DOUBLE:
				rv = new Double(Double.longBitsToDouble(tu.decodeLong(data)));
				break;
			case SPECIAL_BYTEARRAY:
				rv = data;
				break;
			case SPECIAL_STRING:
				rv = decodeString(data);
				break;
			case SPECIAL_STRINGBUFFER:
				rv = new StringBuffer(decodeString(data));
				break;
			case SPECIAL_STRINGBUILDER:
				rv = new StringBuilder(decodeString(data));
				break;
			case SPECIAL_CHARACTER:
				rv = decodeCharacter(data);
				break;
			default:
				log.warn(String.format("Cannot handle data with flags %x", f));
			}
		}
		return rv;
	}

	public CachedData encode(Object o) {
		byte[] b = null;
		int flags = 0;
		if (o instanceof String) {
			b = encodeString((String) o);
			flags |= SPECIAL_STRING;
		} else if (o instanceof StringBuffer) {
			flags |= SPECIAL_STRINGBUFFER;
			b = encodeString(String.valueOf(o));
		} else if (o instanceof StringBuilder) {
			flags |= SPECIAL_STRINGBUILDER;
			b = encodeString(String.valueOf(o));
		} else if (o instanceof Long) {
			b = tu.encodeLong((Long) o);
			flags |= SPECIAL_LONG;
		} else if (o instanceof Integer) {
			b = tu.encodeInt((Integer) o);
			flags |= SPECIAL_INT;
		} else if (o instanceof Short) {
			b = tu.encodeInt((Short) o);
			flags |= SPECIAL_SHORT;
		} else if (o instanceof Boolean) {
			b = this.encodeBoolean((Boolean) o);
			flags |= SPECIAL_BOOLEAN;
		} else if (o instanceof Date) {
			b = tu.encodeLong(((Date) o).getTime());
			flags |= SPECIAL_DATE;
		} else if (o instanceof Byte) {
			b = tu.encodeByte((Byte) o);
			flags |= SPECIAL_BYTE;
		} else if (o instanceof Float) {
			b = tu.encodeInt(Float.floatToIntBits((Float) o));
			flags |= SPECIAL_FLOAT;
		} else if (o instanceof Double) {
			b = tu.encodeLong(Double.doubleToLongBits((Double) o));
			flags |= SPECIAL_DOUBLE;
		} else if (o instanceof byte[]) {
			b = (byte[]) o;
			flags |= SPECIAL_BYTEARRAY;
		} else if (o instanceof Character) {
			b = tu.encodeInt((Character) o);
			flags |= SPECIAL_CHARACTER;
		} else {
			b = serialize(o);
			flags |= SERIALIZED;
		}
		assert b != null;
		if (b.length > compressionThreshold) {
			byte[] compressed = compress(b);
			if (compressed.length < b.length) {
				if (log.isDebugEnabled())
					log
							.debug(String.format("Compressed %s from %d to %d",
									o.getClass().getName(), b.length,
									compressed.length));
				b = compressed;
				flags |= COMPRESSED;
			} else {
				if (log.isDebugEnabled())
					log
							.debug(String
									.format(
											"Compression increased the size of %s from %d to %d",
											o.getClass().getName(), b.length,
											compressed.length));
			}
		}
		return new CachedData(flags, b);
	}

	protected Character decodeCharacter(byte[] b) {
		return Character.valueOf((char) tu.decodeInt(b));
	}

	public byte[] encodeBoolean(boolean b) {
		byte[] rv = new byte[1];
		rv[0] = (byte) (b ? 1 : 0);
		return rv;
	}

	public boolean decodeBoolean(byte[] in) {
		assert in.length == 1 : "Wrong length for a boolean";
		return in[0] == 1;
	}

}
