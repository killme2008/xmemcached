package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;

public class TextIncrDecrCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncodeIncr() {
		Command command = this.commandFactory.createIncrDecrCommand("test",
				"test".getBytes(), 10, 0, 0, CommandType.INCR, false);
		assertNull(command.getIoBuffer());
		command.encode();

		checkByteBufferEquals(command, "incr test 10\r\n");

		command = this.commandFactory.createIncrDecrCommand("test",
				"test".getBytes(), 10, 0, 0, CommandType.INCR, true);
		assertNull(command.getIoBuffer());
		command.encode();

		checkByteBufferEquals(command, "incr test 10 noreply\r\n");
	}

	public void testEncodeDecr() {
		Command command = this.commandFactory.createIncrDecrCommand("test",
				"test".getBytes(), 10, 0, 0, CommandType.DECR, false);
		assertNull(command.getIoBuffer());
		command.encode();

		checkByteBufferEquals(command, "decr test 10\r\n");

		command = this.commandFactory.createIncrDecrCommand("test",
				"test".getBytes(), 10, 0, 0, CommandType.DECR, true);
		assertNull(command.getIoBuffer());
		command.encode();

		checkByteBufferEquals(command, "decr test 10 noreply\r\n");
	}

	public void testDecode() {
		Command command = this.commandFactory.createIncrDecrCommand("test",
				"test".getBytes(), 10, 0, 0, CommandType.DECR, false);
		checkDecodeNullAndNotLineByteBuffer(command);
		// try {
		// command.decode(null, ByteBuffer.wrap("NOT_STORED\r\n".getBytes()));
		// fail();
		// } catch (NumberFormatException e) {
		// assertEquals("For input string: \"NOT_STORED\"", e.getMessage());
		// }
		// try {
		// command.decode(null, ByteBuffer.wrap("test\r\n".getBytes()));
		// fail();
		// } catch (NumberFormatException e) {
		// assertEquals("For input string: \"test\"", e.getMessage());
		// }
		// try {
		// command.decode(null, ByteBuffer.wrap("STORED\r\n".getBytes()));
		// fail();
		// } catch (NumberFormatException e) {
		// assertEquals("For input string: \"STORED\"", e.getMessage());
		// }
		// try {
		// command.decode(null, ByteBuffer.wrap("VALUE test\r\n".getBytes()));
		// fail();
		// } catch (NumberFormatException e) {
		// assertEquals("For input string: \"VALUE test\"", e.getMessage());
		// }
		checkDecodeValidLine(command, "NOT_FOUND\r\n");
		assertEquals("NOT_FOUND", command.getResult());
		checkDecodeValidLine(command, "3\r\n");
		assertEquals(3L, command.getResult());
	}

}
