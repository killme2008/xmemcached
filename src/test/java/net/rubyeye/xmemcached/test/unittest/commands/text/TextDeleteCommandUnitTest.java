package net.rubyeye.xmemcached.test.unittest.commands.text;

import net.rubyeye.xmemcached.command.Command;

public class TextDeleteCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createDeleteCommand("test",
				"test".getBytes(), 10, 0, false);
		assertNull(command.getIoBuffer());
		command.encode();
		this.checkByteBufferEquals(command, "delete test\r\n");

		command = this.commandFactory.createDeleteCommand("test",
				"test".getBytes(), 10, 0, true);
		assertNull(command.getIoBuffer());
		command.encode();
		this.checkByteBufferEquals(command, "delete test noreply\r\n");
	}

	public void testDecode() {
		Command command = this.commandFactory.createDeleteCommand("test",
				"test".getBytes(), 10, 0, false);
		this.checkDecodeNullAndNotLineByteBuffer(command);
		this.checkDecodeInvalidLine(command, "test", "STORED\r\n");
		this.checkDecodeInvalidLine(command, "test", "VALUE test 4 5 1\r\n");
		this.checkDecodeInvalidLine(command, "test", "END\r\n");
		this.checkDecodeValidLine(command, "NOT_FOUND\r\n");
		assertFalse((Boolean) command.getResult());
		command.setResult(null);
		this.checkDecodeValidLine(command, "DELETED\r\n");
		assertTrue((Boolean) command.getResult());
	}
}
