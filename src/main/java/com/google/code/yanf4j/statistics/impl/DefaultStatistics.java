package com.google.code.yanf4j.statistics.impl;

import com.google.code.yanf4j.statistics.Statistics;




/**
 * Ĭ��ͳ��ʵ�֣������κ�����
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:18:55
 */
public class DefaultStatistics implements Statistics {
    public void start() {

    }


    public double getSendBytesPerSecond() {
        return 0;
    }


    public double getReceiveBytesPerSecond() {
        return 0;
    }


    public boolean isStatistics() {
        return false;
    }


    public long getStartedTime() {
        return 0;
    }


    public void reset() {

    }


    public void restart() {

    }


    public double getProcessedMessageAverageTime() {
        return 0;
    }


    public long getProcessedMessageCount() {
        return 0;
    }


    public void statisticsProcess(long n) {

    }


    public void stop() {

    }


    public long getRecvMessageCount() {

        return 0;
    }


    public long getRecvMessageTotalSize() {

        return 0;
    }


    public long getRecvMessageAverageSize() {

        return 0;
    }


    public double getRecvMessageCountPerSecond() {

        return 0;
    }


    public long getWriteMessageCount() {

        return 0;
    }


    public long getWriteMessageTotalSize() {

        return 0;
    }


    public long getWriteMessageAverageSize() {

        return 0;
    }


    public void statisticsRead(long n) {

    }


    public void statisticsWrite(long n) {

    }


    public double getWriteMessageCountPerSecond() {

        return 0;
    }


    public double getAcceptCountPerSecond() {
        return 0;
    }


    public void statisticsAccept() {

    }


    public void setReceiveThroughputLimit(double receivePacketRate) {
    }


    public boolean isReceiveOverFlow() {
        return false;
    }


    public boolean isSendOverFlow() {
        return false;
    }


    public double getSendThroughputLimit() {
        return -1.0;
    }


    public void setSendThroughputLimit(double sendThroughputLimit) {
    }


    public final double getReceiveThroughputLimit() {
        return -1.0;
    }

}
