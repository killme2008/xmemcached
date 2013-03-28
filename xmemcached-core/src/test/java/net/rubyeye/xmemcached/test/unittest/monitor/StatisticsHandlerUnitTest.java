package net.rubyeye.xmemcached.test.unittest.monitor;

import java.util.HashMap;
import java.util.Map;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.monitor.StatisticsHandler;
import junit.framework.TestCase;

public class StatisticsHandlerUnitTest extends TestCase {
	StatisticsHandler handler;

	public void setUp() {
		handler = new StatisticsHandler();
		handler.setStatistics(true);

	}

	public void testStatistics() {
		Map<CommandType, Long> map = new HashMap<CommandType, Long>();
		long i = 0;
		for (CommandType cmdType : CommandType.values()) {
			map.put(cmdType, i++);
		}
		for (CommandType cmdType : CommandType.values()) {
			for (int j = 0; j < map.get(cmdType); j++) {
				handler.statistics(cmdType);
			}
		}

		assertEquals((long) map.get(CommandType.GET_MANY),
				(long) handler.getMultiGetCount());
		assertEquals((long) map.get(CommandType.GETS_MANY),
				(long) handler.getMultiGetsCount());
		assertEquals(
				(long) map.get(CommandType.SET)
						+ (long) map.get(CommandType.SET_MANY),
				(long) handler.getSetCount());
		assertEquals((long) map.get(CommandType.ADD),
				(long) handler.getAddCount());
		assertEquals((long) map.get(CommandType.CAS),
				(long) handler.getCASCount());
		assertEquals((long) map.get(CommandType.REPLACE),
				(long) handler.getReplaceCount());
		assertEquals((long) map.get(CommandType.APPEND),
				(long) handler.getAppendCount());
		assertEquals((long) map.get(CommandType.PREPEND),
				(long) handler.getPrependCount());
		assertEquals((long) map.get(CommandType.DELETE),
				(long) handler.getDeleteCount());
	}
}
