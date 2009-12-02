package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryGetCommandUnitTest extends BaseBinaryCommandUnitTest {
	String key = "hello";
	byte[] keyBytes = ByteUtils.getBytes(key);
	boolean noreply = false;

	public void testGetEncodeAndDecode() {

		Command command = this.commandFactory.createGetCommand(key, keyBytes,
				CommandType.GET_ONE, transcoder);
		command.encode(bufferAllocator);
		ByteBuffer encodeBuffer = command.getIoBuffer().getByteBuffer();
		assertNotNull(encodeBuffer);
		assertEquals(29, encodeBuffer.capacity());

		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.GET.fieldValue(), opCode);

		ByteBuffer buffer = constructResponse(OpCode.GET.fieldValue(),
				(short) 0, (byte) 0x04, (byte) 0, (short) 0, 0x00000009, 0, 1L,
				transcoderUtils.encodeInt(0), null, "world".getBytes());

		assertEquals(33, buffer.capacity());
		assertTrue(command.decode(null, buffer));
		assertEquals("world", transcoder.decode((CachedData) command
				.getResult()));
		assertEquals(0, buffer.remaining());

		buffer = constructResponse(OpCode.GET.fieldValue(), (short) 0,
				(byte) 0x04, (byte) 0, (short) 0, 0x00000004, 0, 1L,
				transcoderUtils.encodeInt(0), null, null);
		assertEquals(28, buffer.capacity());
		command = this.commandFactory.createGetCommand(key, keyBytes,
				CommandType.GET_ONE, transcoder);
		assertTrue(command.decode(null, buffer));
		assertEquals(0, ((CachedData) command.getResult()).getData().length);
		assertEquals(0, buffer.remaining());
	}

}
