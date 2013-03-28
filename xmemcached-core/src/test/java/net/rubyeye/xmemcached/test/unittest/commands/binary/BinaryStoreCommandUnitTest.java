package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryStoreCommandUnitTest extends BaseBinaryCommandUnitTest {
	String key = "hello";
	byte[] keyBytes = ByteUtils.getBytes(this.key);
	String value = "world";
	boolean noreply = false;

	public void testAddEncodeAndDecode() {

		Command command = this.commandFactory.createAddCommand(this.key, this.keyBytes,
				0, this.value, this.noreply, this.transcoder);

		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		assertEquals(42, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.ADD.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.ADD.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 1L, null, null,
				null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.ADD.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 1L, null, null, null);
		command = this.commandFactory.createAddCommand(this.key, this.keyBytes, 0, this.value,
				this.noreply, this.transcoder);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}

	public void testReplaceEncodeAndDecode() {

		Command command = this.commandFactory.createReplaceCommand(this.key,
				this.keyBytes, 0, this.value, this.noreply, this.transcoder);

		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		assertEquals(42, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.REPLACE.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.REPLACE.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 1L, null, null,
				null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.REPLACE.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 1L, null, null, null);
		command = this.commandFactory.createReplaceCommand(this.key, this.keyBytes, 0, this.value,
				this.noreply, this.transcoder);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}

	public void testSetEncodeAndDecode() {

		Command command = this.commandFactory.createSetCommand(this.key, this.keyBytes,
				0, this.value, this.noreply, this.transcoder);

		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		assertEquals(42, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.SET.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.SET.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 1L, null, null,
				null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.SET.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 1L, null, null, null);
		command = this.commandFactory.createSetCommand(this.key, this.keyBytes, 0, this.value,
				this.noreply, this.transcoder);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}

}
