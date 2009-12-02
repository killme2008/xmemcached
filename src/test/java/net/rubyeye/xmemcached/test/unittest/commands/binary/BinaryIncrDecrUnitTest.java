package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryIncrDecrUnitTest extends BaseBinaryCommandUnitTest {

	String key = "counter";
	byte[] keyBytes = ByteUtils.getBytes(key);
	long delta = 0x01;
	long initial = 0x00;
	int exp = 2 * 3600;

	public void testIncrementEncodeDecode() {
		Command command = commandFactory.createIncrDecrCommand(key, keyBytes,
				delta, initial, exp, CommandType.INCR, false);
		command.encode(bufferAllocator);
		ByteBuffer encodeBuffer = command.getIoBuffer().getByteBuffer();
		assertNotNull(encodeBuffer);
		assertEquals(51, encodeBuffer.capacity());
		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.INCREMENT.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.INCREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				transcoderUtils.encodeLong(0L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals((Long)0L,(Long)command.getResult());
		assertEquals(0,buffer.remaining());
		
		command = commandFactory.createIncrDecrCommand(key, keyBytes,
				delta, initial, exp, CommandType.INCR, false);
		buffer = constructResponse(OpCode.INCREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				transcoderUtils.encodeLong(9999L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals((Long)9999L,(Long)command.getResult());
		assertEquals(0,buffer.remaining());

	}
	
	public void testDecrementEncodeDecode() {
		Command command = commandFactory.createIncrDecrCommand(key, keyBytes,
				delta, initial, exp, CommandType.DECR, false);
		command.encode(bufferAllocator);
		ByteBuffer encodeBuffer = command.getIoBuffer().getByteBuffer();
		assertNotNull(encodeBuffer);
		assertEquals(51, encodeBuffer.capacity());
		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.DECREMENT.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.DECREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				transcoderUtils.encodeLong(0L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals((Long)0L,(Long)command.getResult());
		assertEquals(0,buffer.remaining());
		
		command = commandFactory.createIncrDecrCommand(key, keyBytes,
				delta, initial, exp, CommandType.DECR, false);
		buffer = constructResponse(OpCode.DECREMENT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0x00000008, 0, 5L, null, null,
				transcoderUtils.encodeLong(9999L));
		assertEquals(32,buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals((Long)9999L,(Long)command.getResult());
		assertEquals(0,buffer.remaining());

	}

}
