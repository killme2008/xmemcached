package net.rubyeye.xmemcached.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import net.rubyeye.xmemcached.FlowControl;
import net.rubyeye.xmemcached.command.Command;

import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.util.LinkedTransferQueue;

public class FlowControlLinkedTransferQueue extends
		LinkedTransferQueue<WriteMessage> {
	private FlowControl flowControl;

	public FlowControlLinkedTransferQueue(FlowControl flowControl) {
		super();
		this.flowControl = flowControl;
	}

	private void checkPermits(WriteMessage e) {
		if (e.getMessage() instanceof Command) {
			Command cmd = (Command) e.getMessage();
			if (cmd.isNoreply()) {
				int i = 3;
				boolean success = false;
				while (i-- > 0) {
					if (this.flowControl.aquire()) {
						success = true;
						break;
					} else {
						// reduce consuming cpu
						Thread.yield();
					}
				}
				if (!success)
					throw new IllegalStateException(
							"No permit for noreply operation,max="
									+ flowControl.max());
			}
		}
	}

	@Override
	public void put(WriteMessage e) throws InterruptedException {
		checkPermits(e);
		super.put(e);
	}

	@Override
	public boolean offer(WriteMessage e, long timeout, TimeUnit unit)
			throws InterruptedException {
		checkPermits(e);
		return super.offer(e, timeout, unit);
	}

	@Override
	public boolean offer(WriteMessage e) {
		checkPermits(e);
		return super.offer(e);
	}

	@Override
	public void transfer(WriteMessage e) throws InterruptedException {
		checkPermits(e);
		super.transfer(e);
	}

	@Override
	public boolean tryTransfer(WriteMessage e, long timeout, TimeUnit unit)
			throws InterruptedException {
		checkPermits(e);
		return super.tryTransfer(e, timeout, unit);
	}

	@Override
	public boolean tryTransfer(WriteMessage e) {
		checkPermits(e);
		return super.tryTransfer(e);
	}

	@Override
	public WriteMessage take() throws InterruptedException {
		WriteMessage rt = super.take();
		releasePermit(rt);
		return rt;
	}

	@Override
	public WriteMessage poll(long timeout, TimeUnit unit)
			throws InterruptedException {
		WriteMessage rt = super.poll(timeout, unit);
		releasePermit(rt);
		return rt;
	}

	@Override
	public WriteMessage poll() {
		WriteMessage rt = super.poll();
		releasePermit(rt);
		return rt;
	}

	private void releasePermit(WriteMessage rt) {
		if (rt != null) {
			if (rt.getMessage() instanceof Command) {
				Command cmd = (Command) rt.getMessage();
				if (cmd.isNoreply()) {
					this.flowControl.release();
				}
			}

		}
	}

	@Override
	public int drainTo(Collection<? super WriteMessage> c) {
		return super.drainTo(c);
	}

	@Override
	public int drainTo(Collection<? super WriteMessage> c, int maxElements) {
		return super.drainTo(c, maxElements);
	}

	@Override
	public Iterator<WriteMessage> iterator() {
		return super.iterator();
	}

	@Override
	public WriteMessage peek() {
		return super.peek();
	}

	@Override
	public boolean isEmpty() {
		return super.isEmpty();
	}

	@Override
	public boolean hasWaitingConsumer() {
		return super.hasWaitingConsumer();
	}

	@Override
	public int size() {
		return super.size();
	}

	@Override
	public int getWaitingConsumerCount() {
		return super.getWaitingConsumerCount();
	}

	@Override
	public int remainingCapacity() {
		return super.remainingCapacity();
	}

}
