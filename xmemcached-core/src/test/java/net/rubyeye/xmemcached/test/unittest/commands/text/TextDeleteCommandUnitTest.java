package net.rubyeye.xmemcached.test.unittest.commands.text;

import net.rubyeye.xmemcached.command.Command;

public class TextDeleteCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createDeleteCommand("test",
				"test".getBytes(), 10, false);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "delete test\r\n");

		command = this.commandFactory.createDeleteCommand("test",
				"test".getBytes(), 10, true);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "delete test noreply\r\n");
	}

	public void testDecode() {
		Command command = this.commandFactory.createDeleteCommand("test",
				"test".getBytes(), 10, false);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "test", "STORED\r\n");
		checkDecodeInvalidLine(command, "test", "VALUE test 4 5 1\r\n");
		checkDecodeInvalidLine(command, "test", "END\r\n");
		checkDecodeValidLine(command, "NOT_FOUND\r\n");
		assertFalse((Boolean) command.getResult());
		command.setResult(null);
		checkDecodeValidLine(command, "DELETED\r\n");
		assertTrue((Boolean) command.getResult());
	}
}
