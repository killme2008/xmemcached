package net.rubyeye.xmemcached.test.unittest.commands.text;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;

@SuppressWarnings("unchecked")
public class TextStoreCommandUnitTest extends BaseTextCommandUnitTest {
	static final String key = "test";
	static final String value = "10";
	static final int exp = 0;
	static final long cas = 999;
	static final Transcoder transcoder = new StringTranscoder();

	public void testCASEncode() {
		Command command = this.commandFactory.createCASCommand(key,
				key.getBytes(), exp, value, cas, false, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "cas test 0 0 2 999\r\n10\r\n");
		command = this.commandFactory.createCASCommand(key, key.getBytes(), exp,
				value, cas, true, transcoder);
		command.encode();
		checkByteBufferEquals(command, "cas test 0 0 2 999 noreply\r\n10\r\n");
	}

	public void testSetEncode() {
		Command command = this.commandFactory.createSetCommand(key,
				key.getBytes(), exp, value, false, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "set test 0 0 2\r\n10\r\n");
		command = this.commandFactory.createSetCommand(key, key.getBytes(), exp,
				value, true, transcoder);
		command.encode();
		checkByteBufferEquals(command, "set test 0 0 2 noreply\r\n10\r\n");
	}

	public void testAddEncode() {
		Command command = this.commandFactory.createAddCommand(key,
				key.getBytes(), exp, value, false, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "add test 0 0 2\r\n10\r\n");

		command = this.commandFactory.createAddCommand(key, key.getBytes(), exp,
				value, true, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "add test 0 0 2 noreply\r\n10\r\n");
	}

	public void testReplaceEncode() {
		Command command = this.commandFactory.createReplaceCommand(key,
				key.getBytes(), exp, value, false, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "replace test 0 0 2\r\n10\r\n");

		command = this.commandFactory.createReplaceCommand(key, key.getBytes(),
				exp, value, true, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "replace test 0 0 2 noreply\r\n10\r\n");
	}

	public void testAppendEncode() {
		Command command = this.commandFactory.createAppendCommand(key,
				key.getBytes(), value, false, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "append test 0 0 2\r\n10\r\n");

		command = this.commandFactory.createAppendCommand(key, key.getBytes(),
				value, true, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "append test 0 0 2 noreply\r\n10\r\n");
	}

	public void testPrependEncode() {
		Command command = this.commandFactory.createPrependCommand(key,
				key.getBytes(), value, false, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "prepend test 0 0 2\r\n10\r\n");

		command = this.commandFactory.createPrependCommand(key, key.getBytes(),
				value, true, transcoder);
		assertNull(command.getIoBuffer());
		command.encode();
		checkByteBufferEquals(command, "prepend test 0 0 2 noreply\r\n10\r\n");
	}

	public void testCASDecode() {
		Command command = this.commandFactory.createCASCommand(key,
				key.getBytes(), exp, value, cas, false, transcoder);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, key, "VALUE test 4 0 5\r\n");
		checkDecodeInvalidLine(command, key, "DELETED\r\n");
		checkDecodeValidLine(command, "STORED\r\n");
		assertTrue((Boolean) command.getResult());
		command.setResult(null);
		checkDecodeValidLine(command, "EXISTS\r\n");
		assertFalse((Boolean) command.getResult());
		command.setResult(null);
		checkDecodeValidLine(command, "STORED\r\n");
		assertTrue((Boolean) command.getResult());
		command.setResult(null);
		checkDecodeValidLine(command, "NOT_FOUND\r\n");
		assertFalse((Boolean) command.getResult());

	}

	public void testIssue128() {
		// store command
		Command command = this.commandFactory.createSetCommand(key,
				key.getBytes(), exp, value, false, transcoder);
		command.decode(null, ByteBuffer.wrap(
				"SERVER_ERROR out of memory storing object\r\n".getBytes()));
		Exception e = command.getException();
		assertNotNull(e);
		assertEquals("out of memory storing object,key=test", e.getMessage());

		// cas command
		command = this.commandFactory.createCASCommand(key, key.getBytes(), exp,
				value, cas, false, transcoder);
		command.decode(null, ByteBuffer.wrap(
				"SERVER_ERROR out of memory storing object\r\n".getBytes()));
		e = command.getException();
		assertNotNull(e);
		assertEquals("out of memory storing object,key=test", e.getMessage());
	}

	public void testStoreDecode() {
		Command command = this.commandFactory.createSetCommand(key,
				key.getBytes(), exp, value, false, transcoder);
		checkDecodeNullAndNotLineByteBuffer(command);
		checkDecodeInvalidLine(command, key, "EXISTS\r\n");
		checkDecodeInvalidLine(command, key, "END\r\n");
		checkDecodeValidLine(command, "STORED\r\n");
		assertTrue((Boolean) command.getResult());
		command.setResult(null);
		checkDecodeValidLine(command, "NOT_STORED\r\n");
		assertFalse((Boolean) command.getResult());
	}

}
