package net.rubyeye.xmemcached.command;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.spy.memcached.transcoders.CachedData;

public class Command {
	public static final String SPLIT = "\r\n";

	Object key;
	Object flag;
	CachedData[] value;
	Object result;
	CountDownLatch latch;
	CommandType commandType;
	RuntimeException throwable;

	public enum CommandType {
		GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION
	}

	public Command() {
		super();
	}

	public Command(Object key, CachedData[] value, Object result, String cmd,
			CountDownLatch latch) {
		super();
		this.key = key;
		this.value = value;
		this.result = result;
		this.latch = latch;
	}

	public Command(String key, CommandType commandType, CountDownLatch latch) {
		super();
		this.key = key;
		this.commandType = commandType;
		this.latch = latch;
	}

	public Object getFlag() {
		return flag;
	}

	public RuntimeException getThrowable() {
		return throwable;
	}

	public void setThrowable(RuntimeException throwable) {
		this.throwable = throwable;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public Object getKey() {
		return key;
	}

	public void setKey(Object key) {
		this.key = key;
	}

	public CachedData[] getValue() {
		return value;
	}

	public void setValue(CachedData[] value) {
		this.value = value;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public ByteBuffer[] getCmd() {
		return null;
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
