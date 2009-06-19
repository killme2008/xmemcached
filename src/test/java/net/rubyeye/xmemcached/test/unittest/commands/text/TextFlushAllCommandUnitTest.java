package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.text.TextFlushAllCommand;

public class TextFlushAllCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createFlushAllCommand(
				new CountDownLatch(1), 0, false);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		assertEquals(TextFlushAllCommand.FLUSH_ALL, command.getIoBuffer()
				.getByteBuffer());

		command = this.commandFactory.createFlushAllCommand(new CountDownLatch(
				1), 0, true);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		assertEquals("flush_all noreply\r\n", new String(command.getIoBuffer()
				.getByteBuffer().array()));

	}

	public void testDecode() {
		Command command = this.commandFactory.createFlushAllCommand(
				new CountDownLatch(1), 0, false);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "END\r\n");
		checkDecodeInvalidLine(command, "STORED\r\n");
		checkDecodeValidLine(command, "OK\r\n");
	}
}