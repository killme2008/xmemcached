package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.ServerAddressAware;

public class TextVersionCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command versionCommand = this.commandFactory.createVersionCommand(new CountDownLatch(1),null);
		assertNull(versionCommand.getIoBuffer());
		versionCommand.encode();
		assertEquals(ServerAddressAware.VERSION, versionCommand.getIoBuffer()
				.buf());
	}

	public void testDecode() {
		Command versionCommand = this.commandFactory.createVersionCommand(new CountDownLatch(1),null);
		checkDecodeNullAndNotLineByteBuffer(versionCommand);
		checkDecodeInvalidLine(versionCommand, "[version]","test\r\n");

		checkDecodeValidLine(versionCommand, "VERSION\r\n");
		assertEquals("unknown version", versionCommand.getResult());

		checkDecodeValidLine(versionCommand, "VERSION 1.2.5\r\n");
		assertEquals("1.2.5", versionCommand.getResult());
	}
}
