package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.VersionCommand;
import net.rubyeye.xmemcached.exception.MemcachedDecodeException;

public class TextVersionCommandUnitTest extends BaseTextCommandUnitTest {
	private static final String DECODE_ERROR_SESSION_WILL_BE_CLOSED = "decode error,session will be closed";

	public void testEncode() {
		Command versionCommand = this.commandFactory.createVersionCommand();
		assertNull(versionCommand.getIoBuffer());
		versionCommand.encode(this.bufferAllocator);
		assertEquals(VersionCommand.VERSION, versionCommand.getIoBuffer()
				.getByteBuffer());
	}

	public void testDecode() {
		Command versionCommand = this.commandFactory.createVersionCommand();
		assertFalse(versionCommand.decode(null, null));
		assertFalse(versionCommand.decode(null, ByteBuffer.allocate(0)));
		assertFalse(versionCommand.decode(null, ByteBuffer.wrap("test"
				.getBytes())));
		try {
			versionCommand.decode(null, ByteBuffer.wrap("test\r\n".getBytes()));
			fail();
		} catch (MemcachedDecodeException e) {
			assertEquals(DECODE_ERROR_SESSION_WILL_BE_CLOSED, e.getMessage());
		}

		assertTrue(versionCommand.decode(null, ByteBuffer.wrap("VERSION\r\n"
				.getBytes())));
		assertEquals("unknown version", versionCommand.getResult());

		assertTrue(versionCommand.decode(null, ByteBuffer
				.wrap("VERSION 1.2.5\r\n".getBytes())));
		assertEquals("1.2.5", versionCommand.getResult());
	}
}
