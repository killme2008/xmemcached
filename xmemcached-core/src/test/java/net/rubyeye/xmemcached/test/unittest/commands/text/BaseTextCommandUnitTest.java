package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedDecodeException;

public class BaseTextCommandUnitTest extends TestCase {
	static final String DECODE_ERROR_SESSION_WILL_BE_CLOSED = "Decode error,session will be closed";
	protected CommandFactory commandFactory;
	protected BufferAllocator bufferAllocator;

	@Override
	public void setUp() {
		this.bufferAllocator = new SimpleBufferAllocator();
		this.commandFactory = new TextCommandFactory();
	}

	protected void checkDecodeNullAndNotLineByteBuffer(Command command) {
		assertFalse(command.decode(null, null));
		assertFalse(command.decode(null, ByteBuffer.allocate(0)));
		assertFalse(command.decode(null, ByteBuffer.wrap("test".getBytes())));
	}

	protected void checkDecodeInvalidLine(Command command,String key,String invalidLine) {
		try {

			command.decode(null, ByteBuffer.wrap(invalidLine.getBytes()));
			fail();
		} catch (MemcachedDecodeException e) {
			assertTrue(true);
			 assertEquals(DECODE_ERROR_SESSION_WILL_BE_CLOSED + ",key="+key+",server returns="
					+ invalidLine.replace("\r\n", ""), e.getMessage());
		}
	}
	
	public void testDecodeError(){
		
	}

	protected void checkDecodeValidLine(Command command, String validLine) {
		assertTrue(command.decode(null, ByteBuffer.wrap(validLine.getBytes())));
	}

	protected void checkByteBufferEquals(Command command, String line) {
		assertEquals(ByteBuffer.wrap(line.getBytes()), command.getIoBuffer()
				.buf());
	}
}
