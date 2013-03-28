package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.text.TextStatsCommand;

/**
 * 
 * @author dennis
 * 
 */
public class TextStatsCommandUnitTest extends BaseTextCommandUnitTest {
	public void testEncode() {
		Command command = this.commandFactory.createStatsCommand(null,
				new CountDownLatch(1), null);
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals(TextStatsCommand.STATS, command.getIoBuffer().buf());
	}

	public void testItemEncode() {
		Command command = this.commandFactory.createStatsCommand(null,
				new CountDownLatch(1), "items");
		assertNull(command.getIoBuffer());
		command.encode();
		assertEquals(ByteBuffer.wrap("stats items\r\n".getBytes()), command
				.getIoBuffer().buf());
	}

	public void testDecode() {
		Command command = this.commandFactory.createStatsCommand(null,
				new CountDownLatch(1), null);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command,"stats", "OK\r\n");
		checkDecodeValidLine(command, "END\r\n");
		assertFalse(command.decode(null, ByteBuffer
				.wrap("STAT bytes 100\r\nSTAT threads 1\r\n".getBytes())));
		Map<String, String> result = (Map<String, String>) command.getResult();
		assertEquals("100", result.get("bytes"));
		assertEquals("1", result.get("threads"));
		checkDecodeValidLine(command, "STAT connections 5\r\nEND\r\n");
		assertEquals(3, result.size());
		assertEquals("5", result.get("connections"));

	}
}
