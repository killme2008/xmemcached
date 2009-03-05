/**
 *Copyright [2008] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.statistics.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.google.code.yanf4j.statistics.Statistics;

/**
 * 类说明:简单统计类
 * 
 * @author dennis zhuang
 */
public class SimpleStatistics implements Statistics {
	private boolean started = false;

	public boolean isStatistics() {
		return started;
	}

	public synchronized void reset() {
		if (started)
			throw new IllegalStateException();
		this.startTime = this.stopTime = -1;
		this.recvMessageCount.set(0);
		this.recvMessageTotalSize.set(0);
		this.writeMessageCount.set(0);
		this.writeMessageTotalSize.set(0);
		this.processMessageCount.set(0);
		this.processMessageTotalTime.set(0);
		this.acceptCount.set(0);
	}

	public synchronized void restart() {
		stop();
		reset();
		start();
	}

	private long startTime, stopTime = -1;

	private AtomicLong recvMessageCount = new AtomicLong();

	private AtomicLong recvMessageTotalSize = new AtomicLong();

	private AtomicLong writeMessageCount = new AtomicLong();

	private AtomicLong writeMessageTotalSize = new AtomicLong();

	private AtomicLong processMessageCount = new AtomicLong();

	private AtomicLong acceptCount = new AtomicLong();

	private AtomicLong processMessageTotalTime = new AtomicLong();

	public long getStartedTime() {
		return this.startTime;
	}

	public double getProcessedMessageAverageTime() {
		return processMessageCount.get() == 0 ? 0
				: (double) processMessageTotalTime.get()
						/ processMessageCount.get();
	}

	public long getProcessedMessageCount() {
		return processMessageCount.get();
	}

	public void statisticsProcess(long n) {
		if (!started)
			return;
		if (n < 0)
			return;
		processMessageTotalTime.addAndGet(n);
		processMessageCount.incrementAndGet();
	}

	public SimpleStatistics() {

	}

	public synchronized void start() {
		this.startTime = System.currentTimeMillis();
		this.started = true;
	}

	public synchronized void stop() {
		this.stopTime = System.currentTimeMillis();
		this.started = false;
	}

	public void statisticsRead(long n) {
		if (!started)
			return;
		if (n <= 0)
			return;
		recvMessageCount.incrementAndGet();
		recvMessageTotalSize.addAndGet(n);
	}

	public long getRecvMessageCount() {
		return recvMessageCount.get();
	}

	public long getRecvMessageTotalSize() {
		return recvMessageTotalSize.get();
	}

	public long getWriteMessageCount() {
		return writeMessageCount.get();
	}

	public long getWriteMessageTotalSize() {
		return writeMessageTotalSize.get();
	}

	public void statisticsWrite(long n) {
		if (!started)
			return;
		if (n <= 0)
			return;
		writeMessageCount.incrementAndGet();
		writeMessageTotalSize.addAndGet(n);
	}

	public long getRecvMessageAverageSize() {
		return recvMessageCount.get() == 0 ? 0 : recvMessageTotalSize.get()
				/ recvMessageCount.get();
	}

	public double getRecvMessageCountPerSecond() {
		long duration = (this.stopTime == -1) ? (System.currentTimeMillis() - this.startTime)
				: (this.stopTime - this.startTime);
		return duration == 0 ? 0 : (double) recvMessageCount.get() * 1000
				/ duration;
	}

	public double getWriteMessageCountPerSecond() {
		long duration = (this.stopTime == -1) ? (System.currentTimeMillis() - this.startTime)
				: (this.stopTime - this.startTime);
		return duration == 0 ? 0 : (double) this.writeMessageCount.get() * 1000
				/ duration;
	}

	public long getWriteMessageAverageSize() {
		return writeMessageCount.get() == 0 ? 0 : this.writeMessageTotalSize
				.get()
				/ writeMessageCount.get();
	}

	public double getAcceptCountPerSecond() {
		long duration = (this.stopTime == -1) ? (System.currentTimeMillis() - this.startTime)
				: (this.stopTime - this.startTime);
		return duration == 0 ? 0 : (double) this.acceptCount.get() * 1000
				/ duration;
	}

	public void statisticsAccept() {
		if (!started)
			return;
		acceptCount.incrementAndGet();
	}

}
