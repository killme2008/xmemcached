package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import org.junit.Ignore;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.transcoders.TranscoderUtils;

@Ignore
public class BaseBinaryCommandUnitTest extends TestCase {
	protected CommandFactory commandFactory;
	protected Transcoder transcoder;
	TranscoderUtils transcoderUtils = new TranscoderUtils(false);
	protected BufferAllocator bufferAllocator = new SimpleBufferAllocator();

	@Override
	protected void setUp() throws Exception {
		commandFactory = new BinaryCommandFactory();
		this.transcoder = new SerializingTranscoder();
	}

	public ByteBuffer constructResponse(byte opCode, short keyLength,
			byte extraLength, byte dataType, short status, int totalBodyLength,
			int opaque, long cas, byte[] extras, byte[] keyBytes,
			byte[] valueBytes) {

		ByteBuffer result = ByteBuffer.allocate(24 + totalBodyLength);
		result.put((byte) 0x81);
		result.put(opCode);
		result.putShort(keyLength);
		result.put(extraLength);
		result.put(dataType);
		result.putShort(status);
		result.putInt(totalBodyLength);
		result.putInt(opaque);
		result.putLong(cas);
		if (extras != null)
			result.put(extras);
		if (keyBytes != null)
			result.put(keyBytes);
		if (valueBytes != null)
			result.put(valueBytes);
		result.flip();
		return result;

	}

}
