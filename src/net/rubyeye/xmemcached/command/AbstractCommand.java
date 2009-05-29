package net.rubyeye.xmemcached.command;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

/**
 * Base command
 *
 * @author dennis
 *
 */
public abstract class AbstractCommand implements Command {

	protected byte[] key;
	protected volatile Object result;
	protected CountDownLatch latch;
	protected CommandType commandType;
	protected Exception exception;
	protected IoBuffer ioBuffer;
	protected volatile boolean cancel;
	protected volatile OperationStatus status;
	protected int mergeCount = -1;
	protected CachedData storedData;
	@SuppressWarnings("unchecked")
	protected Transcoder transcoder;

	public AbstractCommand() {
		super();
		this.status = OperationStatus.SENDING;
		this.latch = new CountDownLatch(1);
	}

	public AbstractCommand(final CommandType cmdType) {
		this.commandType = cmdType;
		this.status = OperationStatus.SENDING;
	}

	public AbstractCommand(final CommandType cmdType, final CountDownLatch latch) {
		this.commandType = cmdType;
		this.latch = latch;
		this.status = OperationStatus.SENDING;
	}

	public AbstractCommand(byte[] key, CommandType commandType,
			CountDownLatch latch) {
		super();
		this.key = key;
		this.commandType = commandType;
		this.latch = latch;
		this.status = OperationStatus.SENDING;
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

	public final byte[] getKey() {
		return key;
	}

	public final void setKey(byte[] key) {
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
