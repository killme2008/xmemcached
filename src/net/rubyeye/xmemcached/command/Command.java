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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * memcached命令类
 * 
 * @author Administrator
 * 
 */
public class Command {

	public static final String SPLIT = "\r\n";
	private Object key; // 关键字
	private volatile Object result = null; // memcached返回结果
	private CountDownLatch latch;
	private CommandType commandType;
	private Exception exception; // 执行异常
	private IoBuffer ioBuffer;
	private volatile boolean cancel = false;
	private volatile OperationStatus status = null;
	private int mergeCount = -1;
	private CachedData storedData;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;

	/**
	 * 命令类型
	 * 
	 * @author dennis
	 * 
	 */
	public enum CommandType {

		STATS, FLUSH_ALL, GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION, DELETE, VERSION, INCR, DECR, GETS_ONE, GETS_MANY, CAS, APPEND, PREPEND, GET_HIT, GET_MSS;
	}

	public void setCommandType(final CommandType commandType) {
		this.commandType = commandType;
	}

	public int getMergeCount() {
		return mergeCount;
	}

	@SuppressWarnings("unchecked")
	public Transcoder getTranscoder() {
		return transcoder;
	}

	@SuppressWarnings("unchecked")
	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
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

	public final void setStatus(OperationStatus status) {
		this.status = status;
	}

	public final void setIoBuffer(IoBuffer byteBufferWrapper) {
		this.ioBuffer = byteBufferWrapper;
	}

	public List<Command> getMergeCommands() {
		return null;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception throwable) {
		this.exception = throwable;
	}

	public final Object getKey() {
		return key;
	}

	public final void setKey(Object key) {
		this.key = key;
	}

	public final Object getResult() {
		return result;
	}

	public final void setResult(Object result) {
		this.result = result;
	}

	public final IoBuffer getIoBuffer() {
		return this.ioBuffer;
	}

	public String toString() {
		try {
			return new String(this.ioBuffer.getByteBuffer().array(), "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		return "[error]";
	}

	public boolean isCancel() {
		return this.status == OperationStatus.SENDING && cancel;
	}

	public final void cancel() {
		this.cancel = true;
		if (this.ioBuffer != null) {
			this.ioBuffer.free();
		}
	}

	public final CountDownLatch getLatch() {
		return latch;
	}

	public final void countDownLatch() {
		this.latch.countDown();
		if (this.latch.getCount() == 0)
			this.status = OperationStatus.DONE;
	}

	public final CommandType getCommandType() {
		return commandType;
	}

	public final void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public CachedData getStoredData() {
		return storedData;
	}

	public void setStoredData(CachedData storedData) {
		this.storedData = storedData;
	}

}
