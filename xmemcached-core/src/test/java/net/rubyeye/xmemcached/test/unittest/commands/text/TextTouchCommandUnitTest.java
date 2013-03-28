package net.rubyeye.xmemcached.test.unittest.commands.text;

import net.rubyeye.xmemcached.command.Command;

public class TextTouchCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createTouchCommand("test",
				"test".getBytes(), null, 10, false);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "touch test 10\r\n");

		command = this.commandFactory.createTouchCommand("test",
				"test".getBytes(), null, 10, true);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "touch test 10 noreply\r\n");
	}

	public void testDecode() {
		Command command = this.commandFactory.createTouchCommand("test",
				"test".getBytes(), null, 10, false);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "test", "STORED\r\n");
		checkDecodeValidLine(command, "NOT_FOUND\r\n");
		assertFalse((Boolean) command.getResult());
		command.setResult(null);
		checkDecodeValidLine(command, "TOUCHED\r\n");
		assertTrue((Boolean) command.getResult());
	}
}
