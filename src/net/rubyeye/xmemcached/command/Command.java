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

	private  Object key; // 关键字

	private volatile Object result = null; // memcached返回结果

	private CountDownLatch latch;

	private CommandType commandType;

	private MemcachedException throwable; // 执行异常

	private ByteBufferWrapper byteBufferWrapper;

	private volatile boolean cancel = false;

	private volatile OperationStatus status = null;

	private int mergeCount = -1;

	/**
	 * 命令类型
	 * 
	 * @author dennis
	 * 
	 */
	public enum CommandType {
		GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION, DELETE, VERSION, INCR, DECR, GETS_ONE, GETS_MANY, CAS;

	}

	public void setCommandType(final CommandType commandType) {
		this.commandType = commandType;
	}

	public int getMergeCount() {
		return mergeCount;
	}

	public void setMergeCount(final int mergetCount) {
		this.mergeCount = mergetCount;
	}

	public Command() {
		super();
		this.status = OperationStatus.SENDING;
	}

	public Command(final CommandType cmdType) {
		this.commandType = cmdType;
		this.status = OperationStatus.SENDING;
	}

	public Command(final CommandType cmdType, final CountDownLatch latch) {
		this.commandType = cmdType;
		this.latch = latch;
		this.status = OperationStatus.SENDING;
	}

	public Command(Object key, CommandType commandType, CountDownLatch latch) {
		super();
		this.key = key;
		this.commandType = commandType;
		this.latch = latch;
		this.status = OperationStatus.SENDING;
	}

	public OperationStatus getStatus() {
		return status;
	}

	public void setStatus(OperationStatus status) {
		this.status = status;
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

	public boolean isCancel() {
		return this.status == OperationStatus.SENDING && cancel;
	}

	public void cancel() {
		this.cancel = true;
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
