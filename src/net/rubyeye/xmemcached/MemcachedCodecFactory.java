package net.rubyeye.xmemcached;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.util.ByteBufferUtils;

import net.rubyeye.xmemcached.command.Command;
import net.spy.memcached.transcoders.CachedData;

public class MemcachedCodecFactory implements CodecFactory<Command> {
	private static final ByteBuffer SPLIT = ByteBuffer.wrap(Command.SPLIT
			.getBytes());

	private static final ByteBuffer VALUE = ByteBuffer.wrap("VALUE".getBytes());

	private static final ByteBuffer END = ByteBuffer.wrap("END\r\n".getBytes());

	private static final ByteBuffer STORED = ByteBuffer.wrap("STORED\r\n"
			.getBytes());

	private static final ByteBuffer NOT_STORED = ByteBuffer
			.wrap("NOT_STORED\r\n".getBytes());

	private static final ByteBuffer ERROR = ByteBuffer.wrap("ERROR\r\n"
			.getBytes());

	private static final ByteBuffer CLIENT_ERROR = ByteBuffer
			.wrap("CLIENT_ERROR\r\n".getBytes());

	private static final ByteBuffer SERVER_ERROR = ByteBuffer
			.wrap("SERVER_ERROR\r\n".getBytes());

	public Decoder<Command> getDecoder() {
		return new Decoder<Command>() {

			public Command decode(ByteBuffer buffer) {
				int origPos = buffer.position();
				int origLimit = buffer.limit();
				int valueIndex, endIndex, storedIndex, notStoredIndex, errorIndex, clientErrorIndex, serverErrorIndex, min;
				valueIndex = endIndex = storedIndex = notStoredIndex = errorIndex = clientErrorIndex = serverErrorIndex = -1;
				min = -2;
				do {
					valueIndex = ByteBufferUtils.indexOf(buffer, VALUE);
					if (valueIndex == origPos) {
						min = valueIndex;
						break;
					}
					endIndex = ByteBufferUtils.indexOf(buffer, END);
					if (endIndex == origPos) {
						min = endIndex;
						break;
					}
					storedIndex = ByteBufferUtils.indexOf(buffer, STORED);
					if (storedIndex == origPos) {
						min = storedIndex;
						break;
					}
					notStoredIndex = ByteBufferUtils
							.indexOf(buffer, NOT_STORED);
					if (notStoredIndex == origPos) {
						min = notStoredIndex;
						break;
					}
					errorIndex = ByteBufferUtils.indexOf(buffer, ERROR);
					if (errorIndex == origPos) {
						min = errorIndex;
						break;
					}
					clientErrorIndex = ByteBufferUtils.indexOf(buffer,
							CLIENT_ERROR);
					if (clientErrorIndex == origPos) {
						min = clientErrorIndex;
						break;
					}
					serverErrorIndex = ByteBufferUtils.indexOf(buffer,
							SERVER_ERROR);
					if (serverErrorIndex == origPos) {
						min = serverErrorIndex;
						break;
					}
					min = min(valueIndex, endIndex, storedIndex,
							notStoredIndex, errorIndex, clientErrorIndex,
							serverErrorIndex);
				} while (false);
				if (min < 0)
					return null;
				if (valueIndex == min) {
					endIndex = ByteBufferUtils.indexOf(buffer, END, valueIndex);
					// 还没有全部接收到
					if (endIndex < 0)
						return null;
					byte[] bytes = null;
					Command command = new Command();
					List<String> keys = new LinkedList<String>();
					List<CachedData> datas = new LinkedList<CachedData>();
					while (true) {
						bytes = parseLine(buffer, valueIndex);
						int nowPos = buffer.position();
						if (bytes == null || bytes.length == 0)
							return null;
						if (new String(bytes, 0, 3).equals("END")) {
							command.setKey(keys);
							command.setResult(datas);
							return command;
						} else if (new String(bytes, 0, 5).equals("VALUE")) {
							String line = new String(bytes);
							String[] items = line.split(" ");
							keys.add(items[1]);
							int flag = Integer.parseInt(items[2]);
							int dataLen = Integer.parseInt(items[3]);
							byte[] data = new byte[dataLen];
							buffer.get(data);
							datas.add(new CachedData(flag, data, dataLen));
							buffer.position(buffer.position()
									+ SPLIT.remaining());
							valueIndex = buffer.position();
						}
					}

				} else if (endIndex == min) {
					if (parseLine(buffer, endIndex) == null)
						return null;
					return new Command();
				}

				else if (storedIndex == min) {
					if (parseLine(buffer, storedIndex) == null)
						return null;
					return new Command() {
						public Object getResult() {
							return true;
						}

					};
				} else if (notStoredIndex == min) {
					if (parseLine(buffer, notStoredIndex) == null)
						return null;
					return new Command() {
						public Object getResult() {
							return false;
						}

					};
				}
				return null;

			}

			private byte[] parseLine(ByteBuffer buffer, int offset) {
				String result = null;
				int index = ByteBufferUtils.indexOf(buffer, SPLIT, offset);
				if (index >= 0) {
					int limit = buffer.limit();
					buffer.limit(index);
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					result = new String(bytes);
					buffer.limit(limit);
					buffer.position(index + SPLIT.remaining());
					return bytes;

				}
				return null;
			}

			private int min(int a, int... nums) {
				int min = a;
				for (int i = 0; i < nums.length; i++)
					if ((nums[i] < min || min < 0) && nums[i] >= 0)
						min = nums[i];
				return min;
			}

		};
	}

	public Encoder<Command> getEncoder() {
		return new Encoder<Command>() {
			public ByteBuffer[] encode(Command cmd) {
				return cmd.getCmd();
			}

		};
	}

}
