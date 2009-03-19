/**
 *Copyright [2009-2010] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.command;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.rubyeye.xmemcached.buffer.ByteBufferWrapper;
import net.rubyeye.xmemcached.exception.MemcachedException;

/**
 * memcached命令类
 * 
 * @author Administrator
 * 
 */
public class Command {
	public static final String SPLIT = "\r\n";

	Object key; // 关键字

	AtomicReference<Object> result = new AtomicReference<Object>(null); // memcached返回结果

	CountDownLatch latch;

	CommandType commandType;

	MemcachedException throwable; // 执行异常

	ByteBufferWrapper byteBufferWrapper;

	AtomicBoolean cancel = new AtomicBoolean(false);

	AtomicReference<OperationStatus> status = new AtomicReference<OperationStatus>(
			null);

	int mergeCount = -1;

	/**
	 * 命令类型
	 * 
	 * @author dennis
	 * 
	 */
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
		this.status.set(OperationStatus.SENDING);
	}

	public Command(CommandType cmdType) {
		this.commandType = cmdType;
		this.status.set(OperationStatus.SENDING);
	}

	public Command(CommandType cmdType, CountDownLatch latch) {
		this.commandType = cmdType;
		this.latch = latch;
		this.status.set(OperationStatus.SENDING);
	}

	public Command(Object key, CommandType commandType, CountDownLatch latch) {
		super();
		this.key = key;
		this.commandType = commandType;
		this.latch = latch;
		this.status.set(OperationStatus.SENDING);
	}

	public OperationStatus getStatus() {
		return status.get();
	}

	public void setStatus(OperationStatus status) {
		this.status.set(status);
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
		return result.get();
	}

	public void setResult(Object result) {
		this.result.compareAndSet(null, result);
	}

	public ByteBufferWrapper getByteBufferWrapper() {
		return this.byteBufferWrapper;
	}

	public boolean isCancel() {
		return this.status.get() == OperationStatus.SENDING && cancel.get();
	}

	public void cancel() {
		this.cancel.set(true);
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
