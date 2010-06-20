package com.google.code.yanf4j.statistics;

/**
 * ͳ�ƽӿ�
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:19:27
 */
public interface Statistics {

	/**
	 * ��ͳ��ǰ����Ҫ����	
	 */
    public void start();


    /**
     * �ر�ͳ��
     */
    public void stop();


    /**
     * ÿ����յ��ֽ���
     * @return
     */
    public double getReceiveBytesPerSecond();


    /**
     * ÿ�뷢�͵��ֽ���
     * @return
     */
    public double getSendBytesPerSecond();


    /**
     * ͳ�ƴ�����ֽ���
     * @param n
     */
    public abstract void statisticsProcess(long n);


    /**
     * �������Ϣ����
     * @return
     */
    public abstract long getProcessedMessageCount();


    /**
     * ������Ϣ��ƽ��ʱ��
     * @return
     */
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