package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryAppendPrependCommandUnitTest extends
		BaseBinaryCommandUnitTest {
	String key = "hello";
	byte[] keyBytes = ByteUtils.getBytes(key);
	String value = "!";
	boolean noreply = false;

	public void testAppendEncodeAndDecode() {

		Command command = this.commandFactory.createAppendCommand(key,
				keyBytes, value, noreply, transcoder);
		command.encode(bufferAllocator);
		ByteBuffer encodeBuffer = command.getIoBuffer().getByteBuffer();
		assertNotNull(encodeBuffer);
		assertEquals(30, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.APPEND.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.APPEND.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 1L, null, null,
				null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.APPEND.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 1L, null, null, null);
		command = this.commandFactory.createAppendCommand(key, keyBytes, value,
				noreply, transcoder);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}

	public void testPrependEncodeAndDecode() {

		Command command = this.commandFactory.createPrependCommand(key,
				keyBytes, value, noreply, transcoder);
		command.encode(bufferAllocator);
		ByteBuffer encodeBuffer = command.getIoBuffer().getByteBuffer();
		assertNotNull(encodeBuffer);
		assertEquals(30, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.PREPEND.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.PREPEND.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 1L, null, null,
				null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.PREPEND.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 1L, null, null, null);
		command = this.commandFactory.createPrependCommand(key, keyBytes,
				value, noreply, transcoder);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}
}
