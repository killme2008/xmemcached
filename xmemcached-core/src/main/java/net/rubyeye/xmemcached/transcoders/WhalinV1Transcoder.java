package net.rubyeye.xmemcached.transcoders;

import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * Handles old whalin (tested with v1.6) encoding: data type is in the first
 * byte of the value.
 * 
 * @author bpartensky
 * @since Oct 16, 2008
 */
public class WhalinV1Transcoder extends BaseSerializingTranscoder implements
		Transcoder<Object> {

	public static final int SPECIAL_BYTE = 1;
	public static final int SPECIAL_BOOLEAN = 2;
	public static final int SPECIAL_INTEGER = 3;
	public static final int SPECIAL_LONG = 4;
	public static final int SPECIAL_CHARACTER = 5;
	public static final int SPECIAL_STRING = 6;
	public static final int SPECIAL_STRINGBUFFER = 7;
	public static final int SPECIAL_FLOAT = 8;
	public static final int SPECIAL_SHORT = 9;
	public static final int SPECIAL_DOUBLE = 10;
	public static final int SPECIAL_DATE = 11;
	public static final int SPECIAL_STRINGBUILDER = 12;
	public static final int COMPRESSED = 2;
	public static final int SERIALIZED = 8;


	public void setPackZeros(boolean packZeros) {
		throw new UnsupportedOperationException();

	}

	public void setPrimitiveAsString(boolean primitiveAsString) {
		throw new UnsupportedOperationException();
	}
	public boolean isPackZeros() {
		return false;
	}

	public boolean isPrimitiveAsString() {
		return false;
	}


	public CachedData encode(Object o) {
		byte[] b = null;
		int flags = 0;
		if (o instanceof String) {
			b = encodeW1String((String) o);
		} else if (o instanceof StringBuffer) {
			b = encodeStringBuffer((StringBuffer) o);
		} else if (o instanceof StringBuilder) {
			b = encodeStringbuilder((StringBuilder) o);
		} else if (o instanceof Long) {
			b = encodeLong((Long) o);
		} else if (o instanceof Integer) {
			b = encodeInteger((Integer) o);
		} else if (o instanceof Short) {
			b = encodeShort((Short) o);
		} else if (o instanceof Boolean) {
			b = encodeBoolean((Boolean) o);
		} else if (o instanceof Date) {
			b = encodeLong(((Date) o).getTime(), SPECIAL_DATE);
		} else if (o instanceof Byte) {
			b = encodeByte((Byte) o);
		} else if (o instanceof Float) {
			b = encodeFloat((Float) o);
		} else if (o instanceof Double) {
			b = encodeDouble((Double) o);
		} else if (o instanceof byte[]) {
			throw new IllegalArgumentException("Cannot handle byte arrays.");
		} else if (o instanceof Character) {
			b = encodeCharacter((Character) o);
		} else {
			b = serialize(o);
			flags |= SERIALIZED;
		}
		assert b != null;
		if (b.length > this.compressionThreshold) {
			byte[] compressed = compress(b);
			if (compressed.length < b.length) {
				log.debug(String.format("Compressed %s from %d to %d", o
						.getClass().getName(), b.length, compressed.length));
				b = compressed;
				flags |= COMPRESSED;
			} else {
				log.debug(String.format(
						"Compression increased the size of %s from %d to %d", o
								.getClass().getName(), b.length,
						compressed.length));
			}
		}
		return new CachedData(flags, b);
	}

	public Object decode(CachedData d) {
		byte[] data = d.getData();
		Object rv = null;
		if ((d.getFlag() & COMPRESSED) != 0) {
			data = decompress(d.getData());
		}
		if ((d.getFlag() & SERIALIZED) != 0) {
			rv = deserialize(data);
		} else {
			int f = data[0];
			switch (f) {
			case SPECIAL_BOOLEAN:
				rv = decodeBoolean(data);
				break;
			case SPECIAL_INTEGER:
				rv = decodeInteger(data);
				break;
			case SPECIAL_SHORT:
				rv = decodeShort(data);
				break;
			case SPECIAL_LONG:
				rv = decodeLong(data);
				break;
			case SPECIAL_DATE:
				rv = new Date(decodeLong(data));
				break;
			case SPECIAL_BYTE:
				rv = decodeByte(data);
				break;
			case SPECIAL_FLOAT:
				rv = decodeFloat(data);
				break;
			case SPECIAL_DOUBLE:
				rv = decodeDouble(data);
				break;
			case SPECIAL_STRING:
				rv = decodeW1String(data);
				break;
			case SPECIAL_STRINGBUFFER:
				rv = new StringBuffer(decodeW1String(data));
				break;
			case SPECIAL_STRINGBUILDER:
				rv = new StringBuilder(decodeW1String(data));
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

	private Short decodeShort(byte[] data) {
		return Short.valueOf((short) decodeInteger(data).intValue());
	}

	private Byte decodeByte(byte[] in) {
		assert in.length == 2 : "Wrong length for a byte";
		byte value = in[1];
		return Byte.valueOf(value);

	}

	private Integer decodeInteger(byte[] in) {
		assert in.length == 5 : "Wrong length for an int";
		return Integer.valueOf((int) decodeLong(in).longValue());

	}

	private Float decodeFloat(byte[] in) {
		assert in.length == 5 : "Wrong length for a float";
		Integer l = decodeInteger(in);
		return Float.valueOf(Float.intBitsToFloat(l.intValue()));
	}

	private Double decodeDouble(byte[] in) {
		assert in.length == 9 : "Wrong length for a double";
		Long l = decodeLong(in);
		return Double.valueOf(Double.longBitsToDouble(l.longValue()));
	}

	private Boolean decodeBoolean(byte[] in) {
		assert in.length == 2 : "Wrong length for a boolean";
		return Boolean.valueOf(in[1] == 1);
	}

	private Long decodeLong(byte[] in) {
		long rv = 0L;
		for (int idx = 1; idx < in.length; idx++) {
			byte i = in[idx];
			rv = (rv << 8) | (i < 0 ? 256 + i : i);
		}
		return Long.valueOf(rv);
	}

	private Character decodeCharacter(byte[] b) {
		return Character.valueOf((char) decodeInteger(b).intValue());
	}

	private String decodeW1String(byte[] b) {
		try {
			return new String(b, 1, b.length - 1, this.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] encodeByte(Byte value) {
		byte[] b = new byte[2];
		b[0] = SPECIAL_BYTE;
		b[1] = value.byteValue();
		return b;
	}

	private byte[] encodeBoolean(Boolean value) {
		byte[] b = new byte[2];
		b[0] = SPECIAL_BOOLEAN;
		b[1] = (byte) (value.booleanValue() ? 1 : 0);
		return b;
	}

	private byte[] encodeInteger(Integer value) {
		byte[] b = encodeNum(value, 4);
		b[0] = SPECIAL_INTEGER;
		return b;
	}

	private byte[] encodeLong(Long value, int type) {
		byte[] b = encodeNum(value, 8);
		b[0] = (byte) type;
		return b;
	}

	private byte[] encodeLong(Long value) {
		return encodeLong(value, SPECIAL_LONG);
	}

	private byte[] encodeShort(Short value) {
		byte[] b = encodeInteger((int) value.shortValue());
		b[0] = SPECIAL_SHORT;
		return b;
	}

	private byte[] encodeFloat(Float value) {
		byte[] b = encodeInteger(Float.floatToIntBits(value));
		b[0] = SPECIAL_FLOAT;
		return b;
	}

	private byte[] encodeDouble(Double value) {
		byte[] b = encodeLong(Double.doubleToLongBits(value));
		b[0] = SPECIAL_DOUBLE;
		return b;
	}

	private byte[] encodeCharacter(Character value) {
		byte[] result = encodeInteger((int) value.charValue());
		result[0] = SPECIAL_CHARACTER;
		return result;
	}

	private byte[] encodeStringBuffer(StringBuffer value) {
		byte[] b = encodeW1String(value.toString());
		b[0] = SPECIAL_STRINGBUFFER;
		return b;
	}

	private byte[] encodeStringbuilder(StringBuilder value) {
		byte[] b = encodeW1String(value.toString());
		b[0] = SPECIAL_STRINGBUILDER;
		return b;
	}

	private byte[] encodeW1String(String value) {
		byte[] svalue = null;
		try {
			svalue = value.getBytes(this.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		byte[] result = new byte[svalue.length + 1];
		System.arraycopy(svalue, 0, result, 1, svalue.length);
		result[0] = SPECIAL_STRING;
		return result;
	}

	private byte[] encodeNum(long l, int maxBytes) {
		byte[] rv = new byte[maxBytes + 1];

		for (int i = 0; i < rv.length - 1; i++) {
			int pos = rv.length - i - 1;
			rv[pos] = (byte) ((l >> (8 * i)) & 0xff);
		}

		return rv;
	}

}
