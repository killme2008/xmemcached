package net.rubyeye.xmemcached.test.unittest.impl;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.command.text.TextGetOneCommand;
import net.rubyeye.xmemcached.impl.Optimizer;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.SimpleBlockingQueue;

import com.google.code.yanf4j.nio.util.FutureImpl;
import com.google.code.yanf4j.util.LinkedTransferQueue;
import com.google.code.yanf4j.util.SimpleQueue;

@SuppressWarnings("unchecked")
public class OptimezerTest extends TestCase {
	Optimizer optimiezer;

	Queue writeQueue;
	BlockingQueue<Command> executingCmds;

	Command currentCmd;

	private CommandFactory commandFactory;

	@Override
	protected void setUp() throws Exception {
		this.optimiezer = new Optimizer();
		this.commandFactory = new TextCommandFactory();
		this.optimiezer.setBufferAllocator(new SimpleBufferAllocator());
		this.writeQueue = new LinkedTransferQueue<Command>();
		this.executingCmds = new SimpleBlockingQueue<Command>();
		for (int i = 0; i < 10; i++) {
			Command cmd = this.commandFactory.createGetCommand(String
					.valueOf(i), String.valueOf(i).getBytes(),
					CommandType.GET_ONE);
			cmd.encode(new SimpleBufferAllocator());
			this.writeQueue.add(cmd);
			cmd.setWriteFuture(new FutureImpl<Boolean>());
		}
		this.currentCmd = (Command) this.writeQueue.poll();
		this.currentCmd.encode(new SimpleBufferAllocator());
	}

	public void testOptimiezeGet() {

		TextGetOneCommand optimiezeCommand = (TextGetOneCommand)this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);

		assertEquals(10, optimiezeCommand.getMergeCommands().size());
		assertEquals(10, optimiezeCommand.getMergeCount());
		assertEquals(0, this.writeQueue.size());
		assertNull(this.writeQueue.peek());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertEquals(10, optimiezeCommand.getMergeCount());
		assertEquals("get 0 1 2 3 4 5 6 7 8 9\r\n", new String(optimiezeCommand
				.getIoBuffer().getByteBuffer().array()));
	}

	public void testOptimiezeGetWithSameKey() {
		this.writeQueue.clear();
		Queue<Command> localQueue = new SimpleQueue<Command>();
		for (int i = 0; i < 10; i++) {
			Command cmd = this.commandFactory.createGetCommand(String
					.valueOf(0), String.valueOf(0).getBytes(),
					CommandType.GET_ONE);
			cmd.encode(new SimpleBufferAllocator());
			this.writeQueue.add(cmd);
			cmd.setWriteFuture(new FutureImpl<Boolean>());
			localQueue.add(cmd);
		}
		TextGetOneCommand optimiezeCommand =(TextGetOneCommand) this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);

		assertEquals(1, optimiezeCommand.getMergeCommands().size());
		assertEquals(11, optimiezeCommand.getMergeCount());
		assertEquals(0, this.writeQueue.size());
		assertNull(this.writeQueue.peek());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertEquals(11, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(optimiezeCommand.getIoBuffer()
				.getByteBuffer().array()));
		optimiezeCommand.decode(null, ByteBuffer.wrap("VALUE 0 0 2\r\n10\r\n"
				.getBytes()));

		assertEquals(0, this.currentCmd.getLatch().getCount());
		Transcoder transcoder = new SerializingTranscoder();
		assertEquals("10", transcoder.decode((CachedData) this.currentCmd
				.getResult()));
		for (Command cmd : localQueue) {
			assertEquals(0, cmd.getLatch().getCount());
			assertEquals("10", transcoder.decode((CachedData) this.currentCmd
					.getResult()));
		}
		assertEquals(0, optimiezeCommand.getMergeCount());
	}

	public void testMergeFactorDecrease() {
		this.optimiezer.setMergeFactor(5);
		TextGetOneCommand optimiezeCommand =(TextGetOneCommand) this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);

		assertEquals(5, optimiezeCommand.getMergeCommands().size());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertEquals(5, optimiezeCommand.getMergeCount());
		assertEquals("get 0 1 2 3 4\r\n", new String(optimiezeCommand
				.getIoBuffer().getByteBuffer().array()));
		assertEquals(5, this.writeQueue.size()); // remain five commands
	}

	public void testMergeFactorEqualsZero() {
		this.optimiezer.setMergeFactor(0);
		TextGetOneCommand optimiezeCommand =(TextGetOneCommand) this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);
		optimiezeCommand.encode(new SimpleBufferAllocator());

		assertNull(optimiezeCommand.getMergeCommands());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertNull(optimiezeCommand.getMergeCommands());
		assertEquals(-1, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(optimiezeCommand.getIoBuffer()
				.getByteBuffer().array()));
		assertEquals(9, this.writeQueue.size());
		assertSame(this.currentCmd, optimiezeCommand);
	}

	public void testDisableMergeGet() {
		this.optimiezer.setOptimizeGet(false); // disable merge get
		TextGetOneCommand optimiezeCommand =(TextGetOneCommand) this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);
		optimiezeCommand.encode(new SimpleBufferAllocator());
		assertNull(optimiezeCommand.getMergeCommands());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertNull(optimiezeCommand.getMergeCommands());
		assertEquals(-1, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(optimiezeCommand.getIoBuffer()
				.getByteBuffer().array()));
		assertEquals(9, this.writeQueue.size());
		assertSame(this.currentCmd, optimiezeCommand);
	}

	public void testMergeDifferenceCommands() {
		this.writeQueue.clear();
		// send five get operation,include current command
		for (int i = 0; i < 5; i++) {
			Command cmd = this.commandFactory.createGetCommand(String
					.valueOf(i), String.valueOf(i).getBytes(),
					CommandType.GET_ONE);
			this.writeQueue.add(cmd);
			cmd.setWriteFuture(new FutureImpl<Boolean>());
		}
		// send five delete operation
		for (int i = 5; i < 10; i++) {
			Command cmd = this.commandFactory.createDeleteCommand(String
					.valueOf(i), String.valueOf(i).getBytes(), 0, false);
			this.writeQueue.add(cmd);
			cmd.setWriteFuture(new FutureImpl<Boolean>());
		}
		// merge five get commands at most
		TextGetOneCommand optimiezeCommand =(TextGetOneCommand) this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);

		assertEquals(5, optimiezeCommand.getMergeCommands().size());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertEquals(6, optimiezeCommand.getMergeCount());
		assertEquals("get 0 1 2 3 4\r\n", new String(optimiezeCommand
				.getIoBuffer().getByteBuffer().array()));
		assertEquals(5, this.writeQueue.size()); // remain five commands

	}

	public void testMergeGetCommandsWithEmptyWriteQueue() {
		this.writeQueue.clear();
		Command optimiezeCommand = this.optimiezer.optimiezeGet(
				this.writeQueue, this.executingCmds, this.currentCmd);
		optimiezeCommand.encode(new SimpleBufferAllocator());
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertSame(this.currentCmd, optimiezeCommand);
		assertTrue(mergeBuffer.remaining() < 100);
		assertSame(mergeBuffer, this.currentCmd.getIoBuffer().getByteBuffer());
		assertEquals(0, this.writeQueue.size());
		assertEquals(-1, optimiezeCommand.getMergeCount());
		assertEquals("get 0\r\n", new String(mergeBuffer.array()));
	}

	public void testMergeLimitBuffer() {
		// set send buffer size to 30,merge four commands at most
		this.optimiezer.setOptimizeMergeBuffer(true);
		Command optimiezeCommand = this.optimiezer.optimiezeMergeBuffer(
				this.currentCmd, this.writeQueue, this.executingCmds, 30);
		assertNotSame(this.currentCmd, optimiezeCommand);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertEquals(0, this.writeQueue.size());
		assertSame(CommandType.GET_ONE, optimiezeCommand.getCommandType());
		assertEquals("get 0\r\nget 1 2 3 4 5 6 7 8 9\r\n", new String(
				mergeBuffer.array())); // current command at last
	}

	public void testMergeAllBuffer() {
		// merge 10 buffers
		this.optimiezer.setOptimizeMergeBuffer(true);
		Command optimiezeCommand = this.optimiezer.optimiezeMergeBuffer(
				this.currentCmd, this.writeQueue, this.executingCmds, 100);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertNotSame(this.currentCmd, optimiezeCommand);
		assertTrue(mergeBuffer.remaining() < 100);
		assertEquals(0, this.writeQueue.size());
		assertEquals("get 0\r\nget 1 2 3 4 5 6 7 8 9\r\n", new String(
				mergeBuffer.array())); // current command at last
	}

	public void testMergeBufferWithEmptyWriteQueue() {
		this.writeQueue.clear();
		Command optimiezeCommand = this.optimiezer.optimiezeMergeBuffer(
				this.currentCmd, this.writeQueue, this.executingCmds, 100);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		assertSame(this.currentCmd, optimiezeCommand);
		assertTrue(mergeBuffer.remaining() < 100);
		assertSame(mergeBuffer, this.currentCmd.getIoBuffer().getByteBuffer());
		assertEquals(0, this.writeQueue.size());
		assertEquals("get 0\r\n", new String(mergeBuffer.array()));

	}

	public void testOptimieze() {
		this.optimiezer.setOptimizeMergeBuffer(true);
		for (int i = 0; i < 10; i++) {
			Command deleteCommand = this.commandFactory.createDeleteCommand(
					String.valueOf(i), String.valueOf(i).getBytes(), 0, false);
			deleteCommand.encode(new SimpleBufferAllocator());
			this.writeQueue.add(deleteCommand);
			deleteCommand.setWriteFuture(new FutureImpl<Boolean>());
		}
		Command optimiezeCommand = this.optimiezer.optimize(this.currentCmd,
				this.writeQueue, this.executingCmds, 16 * 1024);
		ByteBuffer mergeBuffer = optimiezeCommand.getIoBuffer().getByteBuffer();
		StringBuilder sb = new StringBuilder("get ");
		for (int i = 0; i < 10; i++) {
			if (i != 9) {
				sb.append(String.valueOf(i) + " ");
			} else {
				sb.append(String.valueOf(i));
			}
		}
		sb.append("\r\n");
		for (int i = 0; i < 10; i++) {
			sb.append("delete " + String.valueOf(i) + " 0\r\n");
		}
		assertEquals(sb.toString(), new String(mergeBuffer.array()));
		assertEquals(0, this.writeQueue.size());
		assertNull(this.writeQueue.peek());
	}
}
