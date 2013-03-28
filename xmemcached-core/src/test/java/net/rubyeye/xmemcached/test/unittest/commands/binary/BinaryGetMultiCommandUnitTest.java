package net.rubyeye.xmemcached.test.unittest.commands.binary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class BinaryGetMultiCommandUnitTest extends BaseBinaryCommandUnitTest {

	public void testEncodeDecode() {
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			keys.add(String.valueOf(i));
		}

		Command command = this.commandFactory.createGetMultiCommand(keys,
				new CountDownLatch(1), CommandType.GET_MANY, this.transcoder);
		command.encode();
		ByteBuffer encodeBuffer = command.getIoBuffer().buf();
		assertNotNull(encodeBuffer);
		// (header length + key length) x Count
		assertEquals((24 + 1) * keys.size(), encodeBuffer.capacity());

		byte firstOpCode = encodeBuffer.get(1);
		assertEquals(OpCode.GET_KEY_QUIETLY.fieldValue(), firstOpCode);
		byte lastOpCode = encodeBuffer.get(25 * (keys.size() - 1) + 1);
		assertEquals(OpCode.GET_KEY.fieldValue(), lastOpCode);

		// decode
		ByteBuffer[] buffers = new ByteBuffer[10];
		int capacity=0;
		// First 9 buffer is getkq
		for (int i = 0; i < 9; i++) {
			int flag = 0;
			byte[] flagBytes = this.transcoderUtils.encodeInt(flag);
			String key = String.valueOf(i);
			byte[] keyBytes = ByteUtils.getBytes(key);
			if (i % 2 == 0) {
				// value==key
				buffers[i] = constructResponse(OpCode.GET_KEY_QUIETLY
						.fieldValue(), (short) 1, (byte) 0x04, (byte) 0,
						(short) 0, 6, 0, 1L, flagBytes, keyBytes, keyBytes);
			} else {
				// key not found
				buffers[i] = constructResponse(OpCode.GET_KEY_QUIETLY
						.fieldValue(), (short) 1, (byte) 0x04, (byte) 0,
						(short) 0x0001, 5, 0, 1L, flagBytes, keyBytes, null);
			}
			capacity+=buffers[i].capacity();
		}
		// last buffer is getk
		int flag = 0;
		byte[] flagBytes = this.transcoderUtils.encodeInt(flag);
		String key = String.valueOf(9);
		byte[] keyBytes = ByteUtils.getBytes(key);
		buffers[9] = constructResponse(OpCode.GET_KEY.fieldValue(),
				(short) 1, (byte) 0x04, (byte) 0, (short) 0x0001, 5, 0, 1L,
				flagBytes, keyBytes, null);
		
		capacity+=buffers[9].capacity();
		
		ByteBuffer totalBuffer=ByteBuffer.allocate(capacity);
		for(ByteBuffer buffer:buffers){
			totalBuffer.put(buffer);
		}
		totalBuffer.flip();
		
		assertTrue(command.decode(null,totalBuffer));
		Map<String,CachedData> result=(Map<String, CachedData>)command.getResult();
		
		assertNotNull(result);
		assertEquals(5,result.size());
		
		for(int i=0;i<10;i++){
			if(i%2==0){
				assertNotNull(result.get(String.valueOf(i)));
				assertEquals(String.valueOf(i),this.transcoder.decode(result.get(String.valueOf(i))));
			}else{
				assertNull(result.get(String.valueOf(i)));
			}
		}
		
		assertEquals(0,totalBuffer.remaining());
		

	}

}
