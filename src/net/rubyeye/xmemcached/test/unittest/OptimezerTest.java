package net.rubyeye.xmemcached.test.unittest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.google.code.yanf4j.util.Queue;

import net.rubyeye.xmemcached.TextCommandFactory;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.Optimiezer;
import net.rubyeye.xmemcached.utils.SimpleBlockingQueue;
import net.rubyeye.xmemcached.utils.SimpleDeque;
import junit.framework.TestCase;

@SuppressWarnings("unchecked")
public class OptimezerTest extends TestCase {
	Optimiezer optimiezer;

	Queue writeQueue;
	BlockingQueue<Command> executingCmds;

	Command currentCmd;

	@Override
	protected void setUp() throws Exception {
		optimiezer = new Optimiezer();
		optimiezer.setBufferAllocator(new SimpleBufferAllocator());
		writeQueue = new SimpleDeque();
		executingCmds = new SimpleBlockingQueue<Command>();
		for (int i = 0; i < 10; i++)
			writeQueue.add(TextCommandFactory.createGetCommand(String.valueOf(i),
					String.valueOf(i).getBytes(), TextCommandFactory.GET,
					CommandType.GET_ONE));
		currentCmd = (Command) writeQueue.peek();
	}

	public void testOptimiezeGet() {

		Command optimiezeCommand = optimiezer.optimiezeGet(writeQueue,
				executingCmds, currentCmd);

		assertEquals(10, optimiezeCommand.getMergeCommands().size());
		assertEquals(10, optimiezeCommand.getMergeCount());
		assertEquals(1, writeQueue.size());
		assertSame(optimiezeCommand, writeQueue.peek());
		assertSame(CommandType.GET_ONE, optimiezeCommand
				.getCommandType());
		assertEquals(10, optimiezeCommand.getMergeCount());
		assertEquals("get 0 1 2 3 4 5 6 7 8 9\r\n", new String(optimiezeCommand
				.getIoBuffer().getByteBuffer().array()));
	}

	public void testMergeFactorDecrease() {
		optimiezer.setMergeFactor(5);
		Command optimiezeCommand = optimiezer.optimiezeGet(writeQueue,
				executingCmds, currentCmd);

		assertEquals(5, optimiezeCommand.getMergeCommands().size());
		assertSame(CommandType.GET_ONE, optimiezeCommand
				.getCommandType());
		assertEquals(5, optimiezeCommand.getMergeCount());
		assertEquals("get 0 1 2 3 4\r\n", new String(optimiezeCommand
				.getIoBuffer().getByteBuffer().array()));
		assertEquals(6, writeQueue.size()); // remain five commands
	}

	public void testMergeFactorEqualsZero() {
		optimiezer.setMergeFactor(0);
		Command optimiezeCommand = optimiezer.optimiezeGet(writeQueue,
				executingCmds, currentCmd);

		assertNull(optimiezeCommand.getMergeCommands());
		assertSame(CommandType.GET_ONE, optimiezeCommand
				.getCommandType());
		assertNull(optimiezeCommand.getMergeCommands());
		assertEquals(-1, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(optimiezeCommand.getIoBuffer()
				.getByteBuffer().array()));
		assertEquals(10, writeQueue.size());
		assertSame(currentCmd, optimiezeCommand);
	}

	public void testDisableMergeGet() {
		optimiezer.setOptimiezeGet(false); // disable merge get
		Command optimiezeCommand = optimiezer.optimiezeGet(writeQueue,
				executingCmds, currentCmd);

		assertNull(optimiezeCommand.getMergeCommands());
		assertSame(CommandType.GET_ONE, optimiezeCommand
				.getCommandType());
		assertNull(optimiezeCommand.getMergeCommands());
		assertEquals(-1, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(optimiezeCommand.getIoBuffer()
				.getByteBuffer().array()));
		assertEquals(10, writeQueue.size());
		assertSame(currentCmd, optimiezeCommand);
	}

	public void testMergeDifferenceCommands() {
		writeQueue.clear();
		// send five get operation,include current command
		for (int i = 0; i < 5; i++)
			writeQueue.add(TextCommandFactory.createGetCommand(String.valueOf(i),
					String.valueOf(i).getBytes(), TextCommandFactory.GET,
					CommandType.GET_ONE));
		// send five delete operation
		for (int i = 5; i < 10; i++)
			writeQueue.add(TextCommandFactory.createDeleteCommand(
					String.valueOf(i), String.valueOf(i).getBytes(), 0));
		// merge five get commands at most
		Command optimiezeCommand = optimiezer.optimiezeGet(writeQueue,
				executingCmds, currentCmd);

		assertEquals(5, optimiezeCommand.getMergeCommands().size());
		assertSame(CommandType.GET_ONE, optimiezeCommand
				.getCommandType());
		assertEquals(5, optimiezeCommand.getMergeCount());
		assertEquals("get 0 1 2 3 4\r\n", new String(optimiezeCommand
				.getIoBuffer().getByteBuffer().array()));
		assertEquals(5 + 1, writeQueue.size()); // remain five commands

	}

	public void testMergeGetCommandsWithEmptyWriteQueue() {
		writeQueue.clear();
		writeQueue.add(currentCmd);
		Command optimiezeCommand = optimiezer.optimiezeGet(writeQueue,
				executingCmds, currentCmd);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertSame(currentCmd, optimiezeCommand);
		assertTrue(mergeBuffer.remaining() < 100);
		assertSame(mergeBuffer, currentCmd.getIoBuffer().getByteBuffer());
		assertEquals(1, writeQueue.size());
		assertEquals(-1, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(mergeBuffer.array()));
	}

	public void testMergeLimitBuffer() {
		// set send buffer size to 30,merge four commands at most
		Command optimiezeCommand = optimiezer.optimiezeMergeBuffer(currentCmd,
				writeQueue, executingCmds, 30);
		assertNotSame(currentCmd, optimiezeCommand);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertEquals(1, writeQueue.size()); // remain six commands
		assertSame(CommandType.GET_ONE,((Command)writeQueue.peek()).getCommandType());
		assertEquals("get 0\r\nget 1 2 3 4 5 6 7 8 9\r\n", new String(
				mergeBuffer.array())); // current command at last
	}

	public void testMergeAllBuffer() {
		// merge 10 buffers
		Command optimiezeCommand = optimiezer.optimiezeMergeBuffer(currentCmd,
				writeQueue, executingCmds, 100);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertNotSame(currentCmd, optimiezeCommand);
		assertTrue(mergeBuffer.remaining() < 100);
		assertEquals(1, writeQueue.size());
		assertEquals("get 0\r\nget 1 2 3 4 5 6 7 8 9\r\n", new String(
				mergeBuffer.array())); // current command at last
	}

	public void testMergeBufferWithEmptyWriteQueue() {
		writeQueue.clear();
		writeQueue.add(currentCmd);
		Command optimiezeCommand = optimiezer.optimiezeMergeBuffer(currentCmd,
				writeQueue, executingCmds, 100);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertSame(currentCmd, optimiezeCommand);
		assertTrue(mergeBuffer.remaining() < 100);
		assertSame(mergeBuffer, currentCmd.getIoBuffer().getByteBuffer());
		assertEquals(1, writeQueue.size());
		assertEquals("get 0\r\n", new String(mergeBuffer.array()));

	}

	public void testOptimieze() {
		for (int i = 0; i < 10; i++)
			writeQueue.add(TextCommandFactory.createDeleteCommand(
					String.valueOf(i), String.valueOf(i).getBytes(), 0));
		Command optimiezeCommand = optimiezer.optimieze(currentCmd, writeQueue,
				executingCmds, 16 * 1024);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		StringBuilder sb = new StringBuilder("get ");
		for (int i = 0; i < 10; i++)
			if (i != 9)
				sb.append(String.valueOf(i) + " ");
			else
				sb.append(String.valueOf(i));
		sb.append("\r\n");
		for (int i = 0; i < 10; i++)
			sb.append("delete " + String.valueOf(i) + " 0\r\n");
		assertEquals(sb.toString(), new String(mergeBuffer.array()));
		assertEquals(1, writeQueue.size());
		assertSame(optimiezeCommand, writeQueue.peek());
	}
}
