package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryCASCommandUnitTest extends BaseBinaryCommandUnitTest {
	String key = "hello";
	byte[] keyBytes = ByteUtils.getBytes(key);
	String value = "world";
	boolean noreply = false;

	public void testAddEncodeAndDecode() {

		Command command = this.commandFactory.createCASCommand(key, keyBytes,
				0, value, 9L, noreply, transcoder);

		command.encode(bufferAllocator);
		ByteBuffer encodeBuffer = command.getIoBuffer().getByteBuffer();
		assertNotNull(encodeBuffer);
		assertEquals(42, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		// cas use set command
		assertEquals(OpCode.SET.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.SET.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 10L, null,
				null, null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.SET.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 0L, null, null, null);
		command = this.commandFactory.createCASCommand(key, keyBytes, 0, value,
				9L, noreply, transcoder);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}
}
