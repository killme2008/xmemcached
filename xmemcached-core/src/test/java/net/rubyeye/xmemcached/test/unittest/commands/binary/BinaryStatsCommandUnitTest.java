package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.binary.OpCode;

public class BinaryStatsCommandUnitTest extends BaseBinaryCommandUnitTest {
	public void testEncodeDecode() {
		Command command = this.commandFactory.createStatsCommand(
				new InetSocketAddress("localhost", 80), new CountDownLatch(1),
				null);
		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertEquals(24, encodeBuffer.capacity());
		byte opCode = encodeBuffer.get(1);
		assertEquals(OpCode.STAT.fieldValue(), opCode);

		ByteBuffer[] buffers = new ByteBuffer[5];
		int capacity = 0;
		for (int i = 0; i < buffers.length; i++) {
			String key = String.valueOf(i);
			buffers[i] = constructResponse(OpCode.STAT.fieldValue(),
					(short) key.length(), (byte) 0, (byte) 0, (short) 0,
					2 * key.length(), 0, 0L, null, key.getBytes(), key
							.getBytes());
			capacity += buffers[i].capacity();

		}
		ByteBuffer totalBuffer = ByteBuffer.allocate(capacity + 24);
		for (ByteBuffer buffer : buffers) {
			totalBuffer.put(buffer);
		}
		totalBuffer.put(constructResponse(OpCode.STAT.fieldValue(), (short) 0,
				(byte) 0, (byte) 0, (short) 0, 0, 0, 0L, null, null, null));
		totalBuffer.flip();

		assertTrue(command.decode(null, totalBuffer));
		Map<String, String> result = (Map<String, String>) command
				.getResult();

		assertNotNull(result);
		assertTrue(result.size() > 0);
		assertEquals(5, result.size());
		
		for(Map.Entry<String, String> entry:result.entrySet()){
			assertEquals(entry.getKey(),entry.getValue());
		}
		
		
	}
}
