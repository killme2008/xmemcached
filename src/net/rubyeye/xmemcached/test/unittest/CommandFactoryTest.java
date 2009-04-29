package net.rubyeye.xmemcached.test.unittest;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.StringTranscoder;
import net.spy.memcached.transcoders.Transcoder;
import junit.framework.TestCase;

public class CommandFactoryTest extends TestCase {
	public void testCreateDeleteCommand() {
		String key = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		int time = 10;
		Command deleteCmd = CommandFactory.createDeleteCommand("test",
				keyBytes, time);
		assertEquals(Command.CommandType.DELETE, deleteCmd.getCommandType());
		String commandStr = new String(deleteCmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "delete test 10\r\n";

		assertEquals(expectedStr, commandStr);
	}

	public void testCreateVersionCommand() {
		Command versionCmd = CommandFactory.createVersionCommand();
		String commandStr = new String(versionCmd.getIoBuffer().getByteBuffer()
				.array());
		assertEquals("version\r\n", commandStr);
		assertEquals(Command.CommandType.VERSION, versionCmd.getCommandType());
	}

	public void testCreateStoreCommand() {
		String key = "test";
		String value = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		int exp = 0;
		Transcoder transcoder = new StringTranscoder();
		Command storeCmd = CommandFactory.createStoreCommand(key, keyBytes,
				exp, value, Command.CommandType.SET, "set", -1, transcoder);

		assertEquals(Command.CommandType.SET, storeCmd.getCommandType());
		String commandStr = new String(storeCmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "set test " + StringTranscoder.STRING_FLAG
				+ " 0 4\r\ntest\r\n";
		assertEquals(expectedStr, commandStr);
	}

	public void testCreateGetCommand() {
		String key = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		Command getCmd = CommandFactory.createGetCommand(key, keyBytes,
				ByteUtils.GET, Command.CommandType.GET_ONE);
		assertEquals(Command.CommandType.GET_ONE, getCmd.getCommandType());
		String commandStr = new String(getCmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "get test\r\n";
		assertEquals(expectedStr, commandStr);
	}

	public void testCreateIncrDecrCommand() {
		String key = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		int num = 10;
		Command inCr = CommandFactory.createIncrDecrCommand(key, keyBytes, num,
				Command.CommandType.INCR, "incr");
		assertEquals(Command.CommandType.INCR, inCr.getCommandType());
		String commandStr = new String(inCr.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "incr test 10\r\n";
		assertEquals(expectedStr, commandStr);

	}

	public void testCreateGetMultiCommand() {
		List<String> keys = new ArrayList<String>();
		keys.add("a");
		keys.add("b");
		keys.add("c");
		keys.add("a");

		Command cmd = CommandFactory.createGetMultiCommand(keys, null, null,
				ByteUtils.GET, Command.CommandType.GET_MANY, null);
		assertEquals(Command.CommandType.GET_MANY, cmd.getCommandType());
		String commandStr = new String(cmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "get a b c a\r\n";
		assertEquals(expectedStr, commandStr);
	}

}
