package com.google.code.yanf4j.nio.impl;

import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.statistics.impl.DefaultStatistics;
import com.google.code.yanf4j.statistics.impl.SimpleStatistics;
import com.google.code.yanf4j.util.MessageQueue;
import com.google.code.yanf4j.util.Queue;
import com.google.code.yanf4j.nio.Session;

public abstract class SessionFlowController implements ControllerWrapper {
	protected Statistics statistics = new DefaultStatistics();
	protected int statisticsInterval;
	protected double receivePacketRate = -1.0;
	protected int sessionWriteQueueHighWaterMark = -1;
	protected int sessionWriteQueueLowWaterMark = -1;

	public void setReceivePacketRate(double receivePacketRate) {
		if (this.statistics == null
				|| !(this.statistics instanceof SimpleStatistics))
			throw new IllegalStateException(
					"The statisticsServer must be enabled");
		if (receivePacketRate <= 0)
			throw new IllegalArgumentException();
		if (isStarted())
			throw new IllegalStateException();
		this.receivePacketRate = receivePacketRate;
	}

	/**
	 * 是否超过流量控制
	 * 
	 * @return
	 */
	public boolean isOverFlow() {
		if (getReceivePacketRate() <= 0)
			return false;
		return statistics.getRecvMessageCountPerSecond() > getReceivePacketRate();
	}

	public double getReceivePacketRate() {
		return receivePacketRate;
	}

	public void setSessionWriteQueueHighWaterMark(int highWaterMark) {
		if (isStarted())
			throw new IllegalStateException();
		if (this.sessionWriteQueueLowWaterMark > 0
				&& highWaterMark < this.sessionWriteQueueLowWaterMark)
			throw new IllegalArgumentException("lowWaterMark>highWaterMark");
		this.sessionWriteQueueHighWaterMark = highWaterMark;
	}

	public void setSessionWriteQueueLowWaterMark(int lowWaterMark) {
		if (isStarted())
			throw new IllegalStateException();
		if (this.sessionWriteQueueHighWaterMark > 0 && lowWaterMark < 0)
			throw new IllegalArgumentException(
					"highWaterMark>0 and lowWaterMark<0");
		if (this.sessionWriteQueueHighWaterMark > 0
				&& lowWaterMark > this.sessionWriteQueueHighWaterMark)
			throw new IllegalArgumentException("lowWaterMark>highWaterMark");
		this.sessionWriteQueueLowWaterMark = lowWaterMark;
	}

	protected Queue<Session.WriteMessage> buildQueue() {
		Queue<Session.WriteMessage> queue = null;
		if (this.sessionWriteQueueHighWaterMark > 0
				&& this.sessionWriteQueueLowWaterMark > 0)
			queue = new MessageQueue<AbstractSession.WriteMessage>(
					this.sessionWriteQueueLowWaterMark,
					this.sessionWriteQueueHighWaterMark);
		else
			queue = new MessageQueue<Session.WriteMessage>();
		return queue;
	}

}
