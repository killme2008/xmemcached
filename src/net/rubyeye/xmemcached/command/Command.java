package net.rubyeye.xmemcached.command;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.ByteBufferWrapper;
import net.rubyeye.xmemcached.exception.MemcachedException;

public class Command {
	public static final String SPLIT = "\r\n";

	Object key;
	Object result;
	CountDownLatch latch;
	CommandType commandType;
	MemcachedException throwable;
	ByteBufferWrapper byteBufferWrapper;

	int mergeCount = -1;

	public enum CommandType {
		GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION, DELETE, VERSION, INCR, DECR, GETS_ONE, GETS_MANY, CAS;

	}

	public void setCommandType(CommandType commandType) {
		this.commandType = commandType;
	}

	public int getMergeCount() {
		return mergeCount;
	}

	public void setMergeCount(int mergetCount) {
		this.mergeCount = mergetCount;
	}

	public Command() {
		super();
	}

	public Command(CommandType cmdType) {
		this.commandType = cmdType;
	}

	public Command(CommandType cmdType, CountDownLatch latch) {
		this.commandType = cmdType;
		this.latch = latch;
	}

	public Command(Object key, Object result, String cmd, CountDownLatch latch) {
		super();
		this.key = key;
		this.result = result;
		this.latch = latch;
	}

	public Command(Object key, CommandType commandType, CountDownLatch latch) {
		super();
		this.key = key;
		this.commandType = commandType;
		this.latch = latch;
	}

	public void setByteBufferWrapper(ByteBufferWrapper byteBufferWrapper) {
		this.byteBufferWrapper = byteBufferWrapper;
	}

	public List<Command> getMergeCommands() {
		return null;
	}

	public MemcachedException getException() {
		return throwable;
	}

	public void setException(MemcachedException throwable) {
		this.throwable = throwable;
	}

	public Object getKey() {
		return key;
	}

	public void setKey(Object key) {
		this.key = key;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public ByteBufferWrapper getByteBufferWrapper() {
		return this.byteBufferWrapper;
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	public CommandType getCommandType() {
		return commandType;
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}
}
