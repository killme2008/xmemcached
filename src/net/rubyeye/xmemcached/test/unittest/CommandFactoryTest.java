package net.rubyeye.xmemcached.test.unittest;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.TextCommandFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;
import junit.framework.TestCase;

public class CommandFactoryTest extends TestCase {
	public void testCreateDeleteCommand() {
		String key = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		int time = 10;
		Command deleteCmd = TextCommandFactory.createDeleteCommand("test",
				keyBytes, time);
		assertEquals(CommandType.DELETE, deleteCmd.getCommandType());
		String commandStr = new String(deleteCmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "delete test 10\r\n";

		assertEquals(expectedStr, commandStr);
	}

	public void testCreateVersionCommand() {
		Command versionCmd = TextCommandFactory.createVersionCommand();
		String commandStr = new String(versionCmd.getIoBuffer().getByteBuffer()
				.array());
		assertEquals("version\r\n", commandStr);
		assertEquals(CommandType.VERSION, versionCmd.getCommandType());
	}

	public void testCreateStoreCommand() {
		String key = "test";
		String value = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		int exp = 0;
		Transcoder transcoder = new StringTranscoder();
		Command storeCmd = TextCommandFactory.createStoreCommand(key, keyBytes,
				exp, value, CommandType.SET, "set", -1, transcoder);

		assertEquals(CommandType.SET, storeCmd.getCommandType());
		String commandStr = new String(storeCmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "set test " + StringTranscoder.STRING_FLAG
				+ " 0 4\r\ntest\r\n";
		assertEquals(expectedStr, commandStr);
	}

	public void testCreateGetCommand() {
		String key = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		Command getCmd = TextCommandFactory.createGetCommand(key, keyBytes,
				TextCommandFactory.GET, CommandType.GET_ONE);
		assertEquals(CommandType.GET_ONE, getCmd.getCommandType());
		String commandStr = new String(getCmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "get test\r\n";
		assertEquals(expectedStr, commandStr);
	}

	public void testCreateIncrDecrCommand() {
		String key = "test";
		byte[] keyBytes = ByteUtils.getBytes(key);
		int num = 10;
		Command inCr = TextCommandFactory.createIncrDecrCommand(key, keyBytes, num,
				CommandType.INCR, "incr");
		assertEquals(CommandType.INCR, inCr.getCommandType());
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

		Command cmd = TextCommandFactory.createGetMultiCommand(keys, null, null,
				TextCommandFactory.GET, CommandType.GET_MANY, null);
		assertEquals(CommandType.GET_MANY, cmd.getCommandType());
		String commandStr = new String(cmd.getIoBuffer().getByteBuffer()
				.array());

		String expectedStr = "get a b c a\r\n";
		assertEquals(expectedStr, commandStr);
	}

}
