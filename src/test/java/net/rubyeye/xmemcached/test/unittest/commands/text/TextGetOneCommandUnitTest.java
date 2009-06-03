package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

public class TextGetOneCommandUnitTest extends BaseTextCommandUnitTest {
	public void testGetOneEncode() {
		Command command = this.commandFactory.createGetCommand("test", "test"
				.getBytes(), CommandType.GET_ONE);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "get test\r\n");
	}

	public void testGetsOneEncode() {
		Command command = this.commandFactory.createGetCommand("test", "test"
				.getBytes(), CommandType.GETS_ONE);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "gets test\r\n");
	}

	public void testGetOneDecode() {
		Command command = this.commandFactory.createGetCommand("test", "test"
				.getBytes(), CommandType.GET_ONE);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "STORED\r\n");
		checkDecodeInvalidLine(command, "NOT_FOUND\r\n");
		checkDecodeInvalidLine(command, "NOT_STORED\r\n");
		checkDecodeInvalidLine(command, "DELETED\r\n");

		checkDecodeValidLine(command, "END\r\n");
		assertNull(command.getResult());
		assertEquals(0, command.getLatch().getCount());

		command = this.commandFactory.createGetCommand("test", "test"
				.getBytes(), CommandType.GET_ONE);
		assertFalse(command.decode(null, ByteBuffer
				.wrap("VALUE test 0 2\r\n10\r\n".getBytes())));
		assertNull(command.getResult());
		checkDecodeValidLine(command, "VALUE test 0 2\r\n10\r\nEND\r\n");

		assertEquals("10", new String(((CachedData) command.getResult())
				.getData()));
	}

	public void testGetsOneDecode() {
		Command command = this.commandFactory.createGetCommand("test", "test"
				.getBytes(), CommandType.GETS_ONE);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "STORED\r\n");
		checkDecodeInvalidLine(command, "NOT_FOUND\r\n");
		checkDecodeInvalidLine(command, "NOT_STORED\r\n");
		checkDecodeInvalidLine(command, "DELETED\r\n");

		checkDecodeValidLine(command, "END\r\n");
		assertNull(command.getResult());
		assertEquals(0, command.getLatch().getCount());

		command = this.commandFactory.createGetCommand("test", "test"
				.getBytes(), CommandType.GET_ONE);
		assertFalse(command.decode(null, ByteBuffer
				.wrap("VALUE test 0 2 999\r\n10\r\n".getBytes())));
		assertNull(command.getResult());
		checkDecodeValidLine(command, "VALUE test 0 2 999\r\n10\r\nEND\r\n");

		assertEquals("10", new String(((CachedData) command.getResult())
				.getData()));
		assertEquals((long) 999, ((CachedData) command.getResult()).getCas());
	}
}
