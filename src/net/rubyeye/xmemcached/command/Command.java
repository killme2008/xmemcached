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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.code.yanf4j.nio.WriteMessage;

import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * memcached命令类
 * 
 * @author Administrator
 * 
 */
public class Command implements WriteMessage {

	@Override
	public final Object getMessage() {
		return this;
	}

	@Override
	public final ByteBuffer getWriteBuffer() {
		return getIoBuffer().getByteBuffer();
	}

	@Override
	public void setWriteBuffer(ByteBuffer buffers) {
		throw new UnsupportedOperationException();
	}

	private Object key;
	private volatile Object result;
	private CountDownLatch latch;
	private CommandType commandType;
	private Exception exception;
	private IoBuffer ioBuffer;
	private volatile boolean cancel;
	private volatile OperationStatus status;
	private int mergeCount = -1;
	@SuppressWarnings("unchecked")
	private Transcoder transcoder;

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
		if (this.latch != null) {
			this.latch.countDown();
			if (this.latch.getCount() == 0)
				this.status = OperationStatus.DONE;
		}
	}

	public final CommandType getCommandType() {
		return commandType;
	}

	public final void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}
}
