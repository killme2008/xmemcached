package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryTouchCommandUnitTest extends BaseBinaryCommandUnitTest {
	String key = "hello";
	byte[] keyBytes = ByteUtils.getBytes(this.key);

	public void testEncodeDecode() {
		Command command = this.commandFactory.createTouchCommand(key, keyBytes,
				null, 10, false);
		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		assertEquals(24 + 4 + 5, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.TOUCH.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.TOUCH.fieldValue(),
				(short) 0, (byte) 0, (byte) 0, (short) 0, 0, 0, 1L, null, null,
				null);

		assertTrue(command.decode(null, buffer));
		assertTrue((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.TOUCH.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0x0005, 0, 0, 1L, null, null, null);
		command = this.commandFactory.createTouchCommand(key, keyBytes, null,
				10, false);
		assertTrue(command.decode(null, buffer));
		assertFalse((Boolean) command.getResult());
		assertEquals(0, buffer.remaining());
	}

}
