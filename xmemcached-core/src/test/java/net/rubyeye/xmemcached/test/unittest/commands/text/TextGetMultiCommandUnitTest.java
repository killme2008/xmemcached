package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextGetCommand;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;

@SuppressWarnings("unchecked")
public class TextGetMultiCommandUnitTest extends BaseTextCommandUnitTest {
	static final Transcoder transcoder = new SerializingTranscoder();
	static List<String> keys = new ArrayList<String>();
	static {
		keys.add("test1");
		keys.add("test2");
		keys.add("test3");
		keys.add("test4");
	}

	public void testGetManyEncode() {
		Command command = this.commandFactory.createGetMultiCommand(keys,
				new CountDownLatch(1), CommandType.GET_MANY, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();

		checkByteBufferEquals(command, "get test1 test2 test3 test4\r\n");

	}

	public void testGetsManyEncode() {
		Command command = this.commandFactory.createGetMultiCommand(keys,
				new CountDownLatch(1), CommandType.GETS_MANY, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();

		checkByteBufferEquals(command, "gets test1 test2 test3 test4\r\n");
	}

	public void testGetManyDecode() {
		TextGetCommand command = (TextGetCommand) this.commandFactory
				.createGetMultiCommand(keys, new CountDownLatch(1),
						CommandType.GET_MANY, transcoder);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command,"test1", "STORED\r\n");
		checkDecodeInvalidLine(command,"test1", "NOT_FOUND\r\n");
		checkDecodeInvalidLine(command,"test1", "NOT_STORED\r\n");
		checkDecodeInvalidLine(command, "test1","DELETED\r\n");

		checkDecodeValidLine(command, "END\r\n");
		assertEquals(0, ((Map) command.getResult()).size());
		// data not complelte
		command.setParseStatus(net.rubyeye.xmemcached.command.text.TextGetCommand.ParseStatus.NULL);
		assertFalse(command.decode(null, ByteBuffer
				.wrap("VALUE test1 0 2\r\n10\r\nVALUE test2 0 4\r\n10"
						.getBytes())));
		// data coming,but not with END
		assertFalse(command.decode(null, ByteBuffer.wrap("00\r\n".getBytes())));
		checkDecodeValidLine(command, "END\r\n");

		assertEquals(2, ((Map) command.getResult()).size());
		assertEquals("10", transcoder.decode(((Map<String, CachedData>) command
				.getResult()).get("test1")));
		assertEquals("1000", transcoder
				.decode(((Map<String, CachedData>) command.getResult())
						.get("test2")));
	}

	public void testGetsManyDecode() {
		TextGetCommand command = (TextGetCommand) this.commandFactory
				.createGetMultiCommand(keys, new CountDownLatch(1),
						CommandType.GETS_MANY, transcoder);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command,"test1", "STORED\r\n");
		checkDecodeInvalidLine(command,"test1", "NOT_FOUND\r\n");
		checkDecodeInvalidLine(command,"test1", "NOT_STORED\r\n");
		checkDecodeInvalidLine(command, "test1","DELETED\r\n");

		checkDecodeValidLine(command, "END\r\n");
		assertEquals(0, ((Map) command.getResult()).size());
		command.setParseStatus(net.rubyeye.xmemcached.command.text.TextGetCommand.ParseStatus.NULL);
		// data not complelte
		assertFalse(command.decode(null, ByteBuffer
				.wrap("VALUE test1 0 2 999\r\n10\r\nVALUE test2 0 4 1000\r\n10"
						.getBytes())));
		// data coming,but not with END
		assertFalse(command.decode(null, ByteBuffer.wrap("00\r\n".getBytes())));
		checkDecodeValidLine(command, "END\r\n");

		assertEquals(2, ((Map) command.getResult()).size());
		assertEquals(999, ((Map<String, CachedData>) command.getResult()).get(
				"test1").getCas());
		assertEquals(1000, ((Map<String, CachedData>) command.getResult()).get(
				"test2").getCas());
		assertEquals("10", transcoder.decode(((Map<String, CachedData>) command
				.getResult()).get("test1")));
		assertEquals("1000", transcoder
				.decode(((Map<String, CachedData>) command.getResult())
						.get("test2")));
	}
}
