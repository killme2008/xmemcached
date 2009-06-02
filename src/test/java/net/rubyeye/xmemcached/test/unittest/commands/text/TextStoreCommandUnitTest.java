package net.rubyeye.xmemcached.test.unittest.commands.text;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class TextStoreCommandUnitTest extends BaseTextCommandUnitTest {
	static final String key = "test";
	static final String value = "10";
	static final int exp = 0;
	static final long cas = 999;
	static final Transcoder transcoder = new StringTranscoder();

	public void testCASEncode() {
		Command command = this.commandFactory.createCASCommand(key, key
				.getBytes(), exp, value, cas, transcoder);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "cas test 0 0 2 999\r\n10\r\n");
	}

	public void testSetEncode() {
		Command command = this.commandFactory.createSetCommand(key, key
				.getBytes(), exp, value, transcoder);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "set test 0 0 2\r\n10\r\n");
	}

	public void testAddEncode() {
		Command command = this.commandFactory.createAddCommand(key, key
				.getBytes(), exp, value, transcoder);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "add test 0 0 2\r\n10\r\n");
	}

	public void testReplaceEncode() {
		Command command = this.commandFactory.createReplaceCommand(key, key
				.getBytes(), exp, value, transcoder);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "replace test 0 0 2\r\n10\r\n");
	}

	public void testAppendEncode() {
		Command command = this.commandFactory.createAppendCommand(key, key
				.getBytes(), value, transcoder);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "append test 0 0 2\r\n10\r\n");
	}

	public void testPrependEncode() {
		Command command = this.commandFactory.createPrependCommand(key, key
				.getBytes(), value, transcoder);
		assertNull(command.getIoBuffer());
		command.encode(bufferAllocator);
		checkByteBufferEquals(command, "prepend test 0 0 2\r\n10\r\n");
	}

	public void testCASDecode() {
		Command command = this.commandFactory.createCASCommand(key, key
				.getBytes(), exp, value, cas, transcoder);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "NOT_STROED\r\n");
		checkDecodeInvalidLine(command, "END\r\n");
		checkDecodeInvalidLine(command, "DELETED\r\n");
		checkDecodeValidLine(command, "STORED\r\n");
		assertTrue((Boolean) command.getResult());
		checkDecodeValidLine(command, "EXISTS\r\n");
		assertFalse((Boolean) command.getResult());
		checkDecodeValidLine(command, "STORED\r\n");
		assertTrue((Boolean) command.getResult());
		checkDecodeValidLine(command, "NOT_FOUND\r\n");
		assertFalse((Boolean) command.getResult());
	}

	public void testStoreDecode() {
		Command command = this.commandFactory.createSetCommand(key, key
				.getBytes(), exp, value, transcoder);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, "NOT_FOUND\r\n");
		checkDecodeInvalidLine(command, "EXISTS\r\n");
		checkDecodeInvalidLine(command, "END\r\n");
	    checkDecodeValidLine(command, "STORED\r\n");
	    assertTrue((Boolean)command.getResult());
	    checkDecodeValidLine(command, "NOT_STORED\r\n");
	    assertFalse((Boolean)command.getResult());
	}

}
