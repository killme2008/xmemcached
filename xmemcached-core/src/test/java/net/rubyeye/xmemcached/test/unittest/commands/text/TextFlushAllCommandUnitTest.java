package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.text.TextFlushAllCommand;

public class TextFlushAllCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createFlushAllCommand(
				new CountDownLatch(1), 0, false);
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals(TextFlushAllCommand.FLUSH_ALL, command.getIoBuffer()
				.buf());

		command = this.commandFactory.createFlushAllCommand(new CountDownLatch(
				1), 0, true);
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals("flush_all noreply\r\n", new String(command.getIoBuffer()
				.buf().array()));

	}
	
	public void testEncodeWithDelay(){
		Command command = this.commandFactory.createFlushAllCommand(
				new CountDownLatch(1), 10, false);
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals("flush_all 10\r\n", new String(command.getIoBuffer()
				.buf().array()));

		command = this.commandFactory.createFlushAllCommand(new CountDownLatch(
				1), 10, true);
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals("flush_all 10 noreply\r\n", new String(command.getIoBuffer()
				.buf().array()));
	}

	public void testDecode() {
		Command command = this.commandFactory.createFlushAllCommand(
				new CountDownLatch(1), 0, false);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command,"[flush_all]", "END\r\n");
		checkDecodeInvalidLine(command,"[flush_all]", "STORED\r\n");
		checkDecodeValidLine(command, "OK\r\n");
	}
}