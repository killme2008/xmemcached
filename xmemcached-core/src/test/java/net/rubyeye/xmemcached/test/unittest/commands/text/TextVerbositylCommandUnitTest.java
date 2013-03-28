package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;

public class TextVerbositylCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createVerbosityCommand(
				new CountDownLatch(1), 1, false);
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals("verbosity 1\r\n", new String(command.getIoBuffer()
				.buf().array()));

		command = this.commandFactory.createVerbosityCommand(
				new CountDownLatch(1), 1, true);
		command.encode();
		assertEquals("verbosity 1 noreply\r\n", new String(command
				.getIoBuffer().buf().array()));

	}

	public void testDecode() {
		Command command = this.commandFactory.createVerbosityCommand(
				new CountDownLatch(1), 0, false);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command,"[verbosity]", "END\r\n");
		checkDecodeInvalidLine(command,"[verbosity]", "STORED\r\n");
		checkDecodeValidLine(command, "OK\r\n");
	}
}