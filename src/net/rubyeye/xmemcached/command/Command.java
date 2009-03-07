package net.rubyeye.xmemcached.command;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.code.yanf4j.nio.Message;

public class Command implements Message {
	public static final String SPLIT = "\r\n";

	Object key;
	Object result;
	CountDownLatch latch;
	CommandType commandType;
	RuntimeException throwable;
	ByteBuffer byteBuffer;

	int mergeCount = -1;

	public enum CommandType implements Message {
		GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION, DELETE, VERSION, INCR, DECR, GET, STORE, OTHER;

		public int getLength() {
			return 4;
		}

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
	
	

	public void setByteBuffer(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	public List<Command> getMergeCommands() {
		return null;
	}

	public synchronized RuntimeException getException() {
		return throwable;
	}

	public synchronized void setException(RuntimeException throwable) {
		this.throwable = throwable;
	}

	public Object getKey() {
		return key;
	}

	public void setKey(Object key) {
		this.key = key;
	}

	public synchronized Object getResult() {
		return result;
	}

	public synchronized void setResult(Object result) {
		this.result = result;
	}

	public ByteBuffer getByteBuffer() {
		return this.byteBuffer;
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

	public int getLength() {
		return 4;
	}

}
