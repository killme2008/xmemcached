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
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedDecodeException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.FutureImpl;

/**
 * Abstract Memcached Command
 * 
 * @author dennis
 * 
 */
public abstract class Command implements WriteMessage {

	public static final byte REQUEST_MAGIC_NUMBER = (byte) (0x80 & 0xFF);

	public static final byte RESPONSE_MAGIC_NUMBER = (byte) (0x81 & 0xFF);

	public final Object getMessage() {
		return this;
	}

	public synchronized final com.google.code.yanf4j.buffer.IoBuffer getWriteBuffer() {
		return getIoBuffer();
	}

	public void setWriteBuffer(com.google.code.yanf4j.buffer.IoBuffer buffers) {
		// throw new UnsupportedOperationException();
	}

	protected String key;
	protected byte[] keyBytes;
	protected volatile Object result;
	protected CountDownLatch latch;
	protected CommandType commandType;
	protected Exception exception;
	protected volatile IoBuffer ioBuffer;
	protected volatile boolean cancel;
	protected volatile OperationStatus status;
	protected int mergeCount = -1;
	private int copiedMergeCount = mergeCount;
	@SuppressWarnings("unchecked")
	protected Transcoder transcoder;
	protected boolean noreply;
	protected FutureImpl<Boolean> writeFuture;

	public final byte[] getKeyBytes() {
		return keyBytes;
	}

	public final void setKeyBytes(byte[] keyBytes) {
		this.keyBytes = keyBytes;
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
		mergeCount = mergetCount;
		this.copiedMergeCount = mergetCount;
	}

	public int getCopiedMergeCount() {
		return copiedMergeCount;
	}

	public Command() {
		super();
		status = OperationStatus.SENDING;
	}

	public Command(String key, byte[] keyBytes, CountDownLatch latch) {
		super();
		this.key = key;
		this.keyBytes = keyBytes;
		status = OperationStatus.SENDING;
		this.latch = latch;
	}

	public Command(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch) {
		super();
		this.key = key;
		this.keyBytes = keyBytes;
		status = OperationStatus.SENDING;
		this.latch = latch;
		commandType = cmdType;
	}

	public Command(final CommandType cmdType) {
		commandType = cmdType;
		status = OperationStatus.SENDING;
	}

	public Command(final CommandType cmdType, final CountDownLatch latch) {
		commandType = cmdType;
		this.latch = latch;
		status = OperationStatus.SENDING;
	}

	public Command(final String key, final CommandType commandType,
			final CountDownLatch latch) {
		super();
		this.key = key;
		this.commandType = commandType;
		this.latch = latch;
		status = OperationStatus.SENDING;
	}

	public OperationStatus getStatus() {
		return status;
	}

	public final void setStatus(OperationStatus status) {
		this.status = status;
	}

	public final void setIoBuffer(IoBuffer ioBuffer) {
		this.ioBuffer = ioBuffer;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception throwable) {
		exception = throwable;
	}

	public final String getKey() {
		return key;
	}

	public final void setKey(String key) {
		this.key = key;
	}

	public final Object getResult() {
		return result;
	}

	public final void setResult(Object result) {
		this.result = result;
	}

	public final IoBuffer getIoBuffer() {
		return ioBuffer;
	}

	@Override
	public String toString() {
		try {
			return new String(ioBuffer.buf().array(), "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		return "[error]";
	}

	public boolean isCancel() {
		return status == OperationStatus.SENDING && cancel;
	}

	public final void cancel() {
		cancel = true;
		if (ioBuffer != null) {
			ioBuffer.free();
		}
	}

	public final CountDownLatch getLatch() {
		return latch;
	}

	public final void countDownLatch() {
		if (latch != null) {
			latch.countDown();
			if (latch.getCount() == 0) {
				status = OperationStatus.DONE;
			}
		}
	}

	public final CommandType getCommandType() {
		return commandType;
	}

	public final void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public abstract void encode();

	public abstract boolean decode(MemcachedTCPSession session,
			ByteBuffer buffer);

	protected final void decodeError(String msg, Throwable e) {
		throw new MemcachedDecodeException(
				msg == null ? "decode error,session will be closed,key="
						+ this.key : msg, e);
	}

	protected final void decodeError() {
		throw new MemcachedDecodeException(
				"decode error,session will be closed,key=" + this.key);
	}

	protected final boolean decodeError(String line) {
		if (line.startsWith("ERROR")) {
			String[] splits = line.split("ERROR");
			String errorMsg = splits.length >= 2 ? splits[1]
					: "Unknow command " + getCommandType();
			setException(new UnknownCommandException(
					"Response error,error message:" + errorMsg + ",key="
							+ this.key));
			countDownLatch();
			return true;
		} else if (line.startsWith("CLIENT_ERROR")) {
			setException(new MemcachedClientException(getErrorMsg(line,
					"Unknown Client Error")));
			this.countDownLatch();
			return true;
		} else if (line.startsWith("SERVER_ERROR")) {
			setException(new MemcachedServerException(getErrorMsg(line,
					"Unknown Server Error")));
			this.countDownLatch();
			return true;
		} else {
			throw new MemcachedDecodeException(
					"Decode error,session will be closed,key=" + this.key
							+ ",server returns=" + line);
		}

	}

	protected final boolean decodeError(Session session, ByteBuffer buffer) {
		String line = ByteUtils.nextLine(buffer);
		if (line == null) {
			return false;
		} else {
			return decodeError(line);
		}
	}

	private String getErrorMsg(String line, String defaultMsg) {
		int index = line.indexOf(" ");
		String errorMsg = index > 0 ? line.substring(index + 1) : defaultMsg;
		errorMsg += ",key=" + this.key;
		return errorMsg;
	}

	public final boolean isNoreply() {
		return noreply;
	}

	public final void setNoreply(boolean noreply) {
		this.noreply = noreply;
	}

	public FutureImpl<Boolean> getWriteFuture() {
		return writeFuture;
	}

	public final void setWriteFuture(FutureImpl<Boolean> writeFuture) {
		this.writeFuture = writeFuture;
	}

	public final boolean isWriting() {
		return true;
	}

	public final void writing() {
		// do nothing
	}

}
