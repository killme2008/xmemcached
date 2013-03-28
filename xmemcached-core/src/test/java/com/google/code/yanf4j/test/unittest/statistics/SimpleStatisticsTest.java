package com.google.code.yanf4j.test.unittest.statistics;

import junit.framework.TestCase;

import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.statistics.impl.SimpleStatistics;

public class SimpleStatisticsTest extends TestCase {

	Statistics statistics;

	@Override
	protected void setUp() throws Exception {
		statistics = new SimpleStatistics();
		statistics.start();
	}

	@Override
	protected void tearDown() throws Exception {
		statistics.stop();
		assertFalse(statistics.isStatistics());
	}

	public void testRead() throws Exception {
		assertTrue(statistics.isStatistics());
		assertEquals(0, statistics.getRecvMessageCount());
		assertEquals(0.0, statistics.getRecvMessageCountPerSecond());
		assertEquals(0, statistics.getRecvMessageTotalSize());
		assertEquals(0, statistics.getRecvMessageAverageSize());
		statistics.statisticsRead(4096);
		assertEquals(1, statistics.getRecvMessageCount());
		assertEquals(4096, statistics.getRecvMessageTotalSize());
		assertEquals(4096, statistics.getRecvMessageAverageSize());

		statistics.statisticsRead(1024);
		assertEquals(2, statistics.getRecvMessageCount());
		assertEquals(5120, statistics.getRecvMessageTotalSize());
		assertEquals(2560, statistics.getRecvMessageAverageSize());
		Thread.sleep(1000);
		assertEquals(2.0, statistics.getRecvMessageCountPerSecond(), 0.5);

		statistics.statisticsRead(512);
		assertEquals(3, statistics.getRecvMessageCount());
		assertEquals(5632, statistics.getRecvMessageTotalSize());
		assertEquals(1877, statistics.getRecvMessageAverageSize());
		assertEquals(3.0, statistics.getRecvMessageCountPerSecond(), 0.5);
		// 忽略0和负数
		statistics.statisticsRead(0);
		assertEquals(3, statistics.getRecvMessageCount());
		assertEquals(5632, statistics.getRecvMessageTotalSize());
		assertEquals(1877, statistics.getRecvMessageAverageSize());
		assertEquals(3.0, statistics.getRecvMessageCountPerSecond(), 0.5);

		statistics.statisticsRead(-100);
		assertEquals(3, statistics.getRecvMessageCount());
		assertEquals(5632, statistics.getRecvMessageTotalSize());
		assertEquals(1877, statistics.getRecvMessageAverageSize());
		assertEquals(3.0, statistics.getRecvMessageCountPerSecond(), 0.5);
		
		statistics.restart();
		assertEquals(0, statistics.getRecvMessageCount());
		assertEquals(0.0, statistics.getRecvMessageCountPerSecond());
		assertEquals(0, statistics.getRecvMessageTotalSize());
		assertEquals(0, statistics.getRecvMessageAverageSize());
	}

	public void testWrite() throws Exception {
		assertEquals(0, statistics.getWriteMessageCount());
		assertEquals(0.0, statistics.getWriteMessageCountPerSecond());
		assertEquals(0, statistics.getWriteMessageTotalSize());
		assertEquals(0, statistics.getWriteMessageAverageSize());
		statistics.statisticsWrite(4096);
		assertEquals(1, statistics.getWriteMessageCount());
		assertEquals(4096, statistics.getWriteMessageTotalSize());
		assertEquals(4096, statistics.getWriteMessageAverageSize());

		statistics.statisticsWrite(1024);
		assertEquals(2, statistics.getWriteMessageCount());
		assertEquals(5120, statistics.getWriteMessageTotalSize());
		assertEquals(2560, statistics.getWriteMessageAverageSize());
		Thread.sleep(1000);
		assertEquals(2.0, statistics.getWriteMessageCountPerSecond(), 0.5);

		statistics.statisticsWrite(512);
		assertEquals(3, statistics.getWriteMessageCount());
		assertEquals(5632, statistics.getWriteMessageTotalSize());
		assertEquals(1877, statistics.getWriteMessageAverageSize());
		assertEquals(3.0, statistics.getWriteMessageCountPerSecond(), 0.5);
		// 忽略负数和0
		statistics.statisticsWrite(0);
		assertEquals(3, statistics.getWriteMessageCount());
		assertEquals(5632, statistics.getWriteMessageTotalSize());
		assertEquals(1877, statistics.getWriteMessageAverageSize());
		assertEquals(3.0, statistics.getWriteMessageCountPerSecond(), 0.5);
		statistics.statisticsWrite(-1);
		assertEquals(3, statistics.getWriteMessageCount());
		assertEquals(5632, statistics.getWriteMessageTotalSize());
		assertEquals(1877, statistics.getWriteMessageAverageSize());
		assertEquals(3.0, statistics.getWriteMessageCountPerSecond(), 0.5);
	}

	public void testProcess() {
		// 初始状态
		assertEquals(0.0, statistics.getProcessedMessageAverageTime());
		assertEquals(0, statistics.getProcessedMessageCount());
		statistics.statisticsProcess(1500);
		assertEquals(1500.0, statistics.getProcessedMessageAverageTime(), 0.5);
		assertEquals(1, statistics.getProcessedMessageCount());
		// 允许0
		statistics.statisticsProcess(0);
		assertEquals(750.0, statistics.getProcessedMessageAverageTime(), 0.5);
		assertEquals(2, statistics.getProcessedMessageCount());

		statistics.statisticsProcess(987);
		assertEquals(829.0, statistics.getProcessedMessageAverageTime(), 0.5);
		assertEquals(3, statistics.getProcessedMessageCount());
		// 测试负数
		statistics.statisticsProcess(-100);
		assertEquals(829.0, statistics.getProcessedMessageAverageTime(), 0.5);
		assertEquals(3, statistics.getProcessedMessageCount());
	}

}
