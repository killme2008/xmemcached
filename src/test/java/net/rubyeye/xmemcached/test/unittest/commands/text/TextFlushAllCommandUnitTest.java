package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.text.TextFlushAllCommand;

public class TextFlushAllCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory
				.createFlushAllCommand(new CountDownLatch(1));
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		assertEquals(TextFlushAllCommand.FLUSH_ALL, command.getIoBuffer()
				.getByteBuffer());

	}

	public void testDecode() {
		Command command = this.commandFactory
				.createFlushAllCommand(new CountDownLatch(1));
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "END\r\n");
		checkDecodeInvalidLine(command, "STORED\r\n");
		checkDecodeValidLine(command, "OK\r\n");
	}
}