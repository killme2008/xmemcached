package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.BinaryAppendPrependCommand;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;

public class BinaryAppendPrependCommandUnitTest extends TestCase {
	BufferAllocator bufferAllocator;

	@Override
	public void setUp() {
		this.bufferAllocator = new SimpleBufferAllocator();
	}

	public void testEncode() {
		BinaryAppendPrependCommand cmd = createAppendPrependCommand();
		final ByteBuffer encodedBuffer = encodeBuffer(cmd);
		assertEquals(24 + 5 + 1, encodedBuffer.capacity());
		assertEquals(24 + 5 + 1, encodedBuffer.remaining());
	}

	private ByteBuffer encodeBuffer(BinaryAppendPrependCommand cmd) {
		cmd.encode(this.bufferAllocator);

		final ByteBuffer encodedBuffer = cmd.getIoBuffer().getByteBuffer();
		return encodedBuffer;
	}

	private BinaryAppendPrependCommand createAppendPrependCommand() {
		BinaryAppendPrependCommand cmd = new BinaryAppendPrependCommand(
				"hello", "hello".getBytes(), CommandType.APPEND,
				new CountDownLatch(1), 0, 0, "!", false, new StringTranscoder());
		return cmd;
	}

	public void testDecode() {
		BinaryAppendPrependCommand cmd = createAppendPrependCommand();
		assertFalse(cmd.decode(null, this.bufferAllocator.allocate(0).getByteBuffer()));
	     
		
	}
}
