package net.spy.memcached.transcoders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.spy.SpyObject;
import net.spy.util.CloseUtil;

/**
 * Base class for any transcoders that may want to work with serialized or
 * compressed data.
 */
public abstract class BaseSerializingTranscoder extends SpyObject {

	/**
	 * Default compression threshold value.
	 */
	public static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

	private static final String DEFAULT_CHARSET = "UTF-8";

	protected int compressionThreshold=DEFAULT_COMPRESSION_THRESHOLD;
	protected String charset=DEFAULT_CHARSET;


	/**
	 * Set the compression threshold to the given number of bytes.  This
	 * transcoder will attempt to compress any data being stored that's larger
	 * than this.
	 *
	 * @param to the number of bytes
	 */
	public void setCompressionThreshold(int to) {
		compressionThreshold=to;
	}

	/**
	 * Set the character set for string value transcoding (defaults to UTF-8).
	 */
	public void setCharset(String to) {
		// Validate the character set.
		try {
			new String(new byte[97], to);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		charset=to;
	}

	/**
	 * Get the bytes representing the given serialized object.
	 */
	protected byte[] serialize(Object o) {
		if(o == null) {
			throw new NullPointerException("Can't serialize null");
		}
		byte[] rv=null;
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			ObjectOutputStream os=new ObjectOutputStream(bos);
			os.writeObject(o);
			os.close();
			bos.close();
			rv=bos.toByteArray();
		} catch(IOException e) {
			throw new IllegalArgumentException("Non-serializable object", e);
		}
		return rv;
	}

	/**
	 * Get the object represented by the given serialized bytes.
	 */
	protected Object deserialize(byte[] in) {
		Object rv=null;
		try {
			if(in != null) {
				ByteArrayInputStream bis=new ByteArrayInputStream(in);
				ObjectInputStream is=new ObjectInputStream(bis);
				rv=is.readObject();
				is.close();
				bis.close();
			}
		} catch(IOException e) {
			getLogger().warn("Caught IOException decoding %d bytes of data",
					in.length, e);
		} catch (ClassNotFoundException e) {
			getLogger().warn("Caught CNFE decoding %d bytes of data",
					in.length, e);
		}
		return rv;
	}

	/**
	 * Compress the given array of bytes.
	 */
	protected byte[] compress(byte[] in) {
		if(in == null) {
			throw new NullPointerException("Can't compress null");
		}
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		GZIPOutputStream gz=null;
		try {
			gz = new GZIPOutputStream(bos);
			gz.write(in);
		} catch (IOException e) {
			throw new RuntimeException("IO exception compressing data", e);
		} finally {
			CloseUtil.close(gz);
			CloseUtil.close(bos);
		}
		byte[] rv=bos.toByteArray();
		getLogger().debug("Compressed %d bytes to %d", in.length, rv.length);
		return rv;
	}

	/**
	 * Decompress the given array of bytes.
	 *
	 * @return null if the bytes cannot be decompressed
	 */
	protected byte[] decompress(byte[] in) {
		ByteArrayOutputStream bos=null;
		if(in != null) {
			ByteArrayInputStream bis=new ByteArrayInputStream(in);
			bos=new ByteArrayOutputStream();
			GZIPInputStream gis;
			try {
				gis = new GZIPInputStream(bis);

				byte[] buf=new byte[8192];
				int r=-1;
				while((r=gis.read(buf)) > 0) {
					bos.write(buf, 0, r);
				}
			} catch (IOException e) {
				getLogger().warn("Failed to decompress data", e);
				bos = null;
			}
		}
		return bos == null ? null : bos.toByteArray();
	}

	/**
	 * Decode the string with the current character set.
	 */
	protected String decodeString(byte[] data) {
		String rv=null;
		try {
			if(data != null) {
				rv=new String(data, charset);
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return rv;
	}

	/**
	 * Encode a string into the current character set.
	 */
	protected byte[] encodeString(String in) {
		byte[] rv=null;
		try {
			rv=in.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return rv;
	}

}