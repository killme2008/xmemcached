package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryIncrDecrUnitTest extends BaseBinaryCommandUnitTest {

	String key = "counter";
	byte[] keyBytes = ByteUtils.getBytes(this.key);
	long delta = 0x01;
	long initial = 0x00;
	int exp = 2 * 3600;

	public void testIncrementEncodeDecode() {
		Command command = this.commandFactory.createIncrDecrCommand(this.key, this.keyBytes,
				this.delta, this.initial, this.exp, CommandType.INCR, false);
		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		assertEquals(51, encodeBuffer.capacity());
		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.INCREMENT.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.INCREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				this.transcoderUtils.encodeLong(0L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals(0L,command.getResult());
		assertEquals(0,buffer.remaining());
		
		command = this.commandFactory.createIncrDecrCommand(this.key, this.keyBytes,
				this.delta, this.initial, this.exp, CommandType.INCR, false);
		buffer = constructResponse(OpCode.INCREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				this.transcoderUtils.encodeLong(9999L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals(9999L,command.getResult());
		assertEquals(0,buffer.remaining());

	}
	
	public void testDecrementEncodeDecode() {
		Command command = this.commandFactory.createIncrDecrCommand(this.key, this.keyBytes,
				this.delta, this.initial, this.exp, CommandType.DECR, false);
		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		assertEquals(51, encodeBuffer.capacity());
		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.DECREMENT.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.DECREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				this.transcoderUtils.encodeLong(0L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals(0L,command.getResult());
		assertEquals(0,buffer.remaining());
		
		command = this.commandFactory.createIncrDecrCommand(this.key, this.keyBytes,
				this.delta, this.initial, this.exp, CommandType.DECR, false);
		buffer = constructResponse(OpCode.DECREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				this.transcoderUtils.encodeLong(9999L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals(9999L,command.getResult());
		assertEquals(0,buffer.remaining());

	}

}
