/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.statistics;

/**
 * Statistics
 * 
 * @author dennis
 * 
 */
public interface Statistics {

	public void start();

	public void stop();

	public double getReceiveBytesPerSecond();

	public double getSendBytesPerSecond();

	public abstract void statisticsProcess(long n);

	public abstract long getProcessedMessageCount();

	public abstract double getProcessedMessageAverageTime();

	public abstract void statisticsRead(long n);

	public abstract void statisticsWrite(long n);

	public abstract long getRecvMessageCount();

	public abstract long getRecvMessageTotalSize();

	public abstract long getRecvMessageAverageSize();

	public abstract long getWriteMessageTotalSize();

	public abstract long getWriteMessageCount();

	public abstract long getWriteMessageAverageSize();

	public abstract double getRecvMessageCountPerSecond();

	public abstract double getWriteMessageCountPerSecond();

	public void statisticsAccept();

	public double getAcceptCountPerSecond();

	public long getStartedTime();

	public void reset();

	public void restart();

	public boolean isStatistics();

	public void setReceiveThroughputLimit(double receiveThroughputLimit);

	/**
	 * Check session if receive bytes per second is over flow controll
	 * 
	 * @return
	 */
	public boolean isReceiveOverFlow();

	/**
	 * Check session if receive bytes per second is over flow controll
	 * 
	 * @return
	 */
	public boolean isSendOverFlow();

	public double getSendThroughputLimit();

	public void setSendThroughputLimit(double sendThroughputLimit);

	public double getReceiveThroughputLimit();

}