package net.rubyeye.xmemcached;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.util.ByteBufferPattern;
import com.google.code.yanf4j.util.ByteBufferUtils;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.spy.memcached.transcoders.CachedData;

public class MemcachedCodecFactory implements CodecFactory<Command> {
	private static final ByteBuffer SPLIT = ByteBuffer.wrap(Command.SPLIT
			.getBytes());

	private static final ByteBufferPattern SPLIT_PATTERN = ByteBufferPattern
			.compile(SPLIT);

	private static final ByteBufferPattern VALUE_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("VALUE".getBytes()));

	private static final ByteBufferPattern END_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("END\r\n".getBytes()));

	private static final ByteBufferPattern STORED_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("STORED\r\n".getBytes()));

	private static final ByteBufferPattern NOT_STORED_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("NOT_STORED\r\n".getBytes()));

	private static final ByteBufferPattern ERROR_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("ERROR\r\n".getBytes()));

	private static final ByteBufferPattern CLIENT_ERROR_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("CLIENT_ERROR\r\n".getBytes()));

	private static final ByteBufferPattern SERVER_ERROR_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("SERVER_ERROR\r\n".getBytes()));

	private static final ByteBufferPattern DELETED_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("DELETED\r\n".getBytes()));

	private static final ByteBufferPattern NOT_FOUND_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("NOT_FOUND\r\n".getBytes()));

	private static final ByteBufferPattern VERSION_PATTERN = ByteBufferPattern
			.compile(ByteBuffer.wrap("VERSION".getBytes()));

	// todo 匹配算法仍然比较低效，有时间改成KMP算法
	public Decoder<Command> getDecoder() {
		return new Decoder<Command>() {

			public Command decode(ByteBuffer buffer) {
				int origPos = buffer.position();
				// int origLimit = buffer.limit();
				int valueIndex, endIndex, storedIndex, notStoredIndex, errorIndex, clientErrorIndex, serverErrorIndex, min, deletedIndex, notFoundIndex, versionIndex;
				valueIndex = endIndex = storedIndex = notStoredIndex = errorIndex = clientErrorIndex = serverErrorIndex = notFoundIndex = deletedIndex = versionIndex = -1;
				min = -2;
				do {
					valueIndex = ByteBufferUtils.kmpIndexOf(buffer, VALUE_PATTERN);
					if (valueIndex == origPos) {
						min = valueIndex;
						break;
					}
					endIndex = ByteBufferUtils.kmpIndexOf(buffer, END_PATTERN);
					if (endIndex == origPos) {
						min = endIndex;
						break;
					}
					storedIndex = ByteBufferUtils.kmpIndexOf(buffer, STORED_PATTERN);
					if (storedIndex == origPos) {
						min = storedIndex;
						break;
					}
					notStoredIndex = ByteBufferUtils.kmpIndexOf(buffer,
							NOT_STORED_PATTERN);
					if (notStoredIndex == origPos) {
						min = notStoredIndex;
						break;
					}
					deletedIndex = ByteBufferUtils.kmpIndexOf(buffer, DELETED_PATTERN);
					if (deletedIndex == origPos) {
						min = deletedIndex;
						break;
					}
					notFoundIndex = ByteBufferUtils.kmpIndexOf(buffer,
							NOT_FOUND_PATTERN);
					if (notFoundIndex == origPos) {
						min = notFoundIndex;
						break;
					}
					errorIndex = ByteBufferUtils.kmpIndexOf(buffer, ERROR_PATTERN);
					if (errorIndex == origPos) {
						min = errorIndex;
						break;
					}
					clientErrorIndex = ByteBufferUtils.kmpIndexOf(buffer,
							CLIENT_ERROR_PATTERN);
					if (clientErrorIndex == origPos) {
						min = clientErrorIndex;
						break;
					}
					serverErrorIndex = ByteBufferUtils.kmpIndexOf(buffer,
							SERVER_ERROR_PATTERN);
					if (serverErrorIndex == origPos) {
						min = serverErrorIndex;
						break;
					}
					versionIndex = ByteBufferUtils.kmpIndexOf(buffer, VERSION_PATTERN);
					if (versionIndex == origPos) {
						min = versionIndex;
						break;
					}
					min = min(valueIndex, endIndex, storedIndex,
							notStoredIndex, errorIndex, clientErrorIndex,
							serverErrorIndex);
				} while (false);
				// System.out.printf("valueIndex=%d, endIndex=%d,
				// storedIndex=%d, notStoredIndex=%d, errorIndex=%d,
				// clientErrorIndex=%d, serverErrorIndex=%d, min=%d,
				// deletedIndex=%d, notFoundIndex=%d, versionIndex=%d,min=%d\n",
				// valueIndex, endIndex, storedIndex, notStoredIndex,
				// errorIndex, clientErrorIndex, serverErrorIndex, min,
				// deletedIndex, notFoundIndex, versionIndex,min);
				if (min < 0 && buffer.remaining() == 0)
					return null;
				else if (min >= 0 && buffer.remaining() > 0) {
					if (valueIndex == min) {
						endIndex = ByteBufferUtils.kmpIndexOf(buffer, END_PATTERN,
								valueIndex);
						if (endIndex < 0)
							return null;
						byte[] bytes = null;
						Command command = new Command(Command.CommandType.GET);
						List<String> keys = new ArrayList<String>(30);
						List<CachedData> datas = new ArrayList<CachedData>(30);
						while (true) {
							bytes = parseLine(buffer, valueIndex);
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
						return parseEndCommand(buffer, endIndex);
					}

					else if (storedIndex == min) {
						return parseStored(buffer, storedIndex);
					} else if (notStoredIndex == min) {
						return parseNotStored(buffer, notStoredIndex);
					} else if (deletedIndex == min) {
						return parseDeleted(buffer, deletedIndex);
					} else if (notFoundIndex == min) {
						return parseNotFound(buffer, notFoundIndex);
					} else if (errorIndex == min) {
						return parseException(buffer, errorIndex);
					} else if (clientErrorIndex == min) {
						return parseClientException(buffer, clientErrorIndex);
					} else if (serverErrorIndex == min) {
						return parseServerException(buffer, serverErrorIndex);
					} else if (versionIndex == min) {
						return parseVersionCommand(buffer, versionIndex);
					}
				} else if (min < 0 && buffer.remaining() > 0) {
					// incr,decr命令
					return parseIncrDecrCommand(buffer, origPos);
				} else
					return null;
				return null;
			}

			private Command parseEndCommand(ByteBuffer buffer, int endIndex) {
				if (parseLine(buffer, endIndex) == null)
					return null;
				return new Command(Command.CommandType.GET);
			}

			private Command parseStored(ByteBuffer buffer, int storedIndex) {
				if (parseLine(buffer, storedIndex) == null)
					return null;
				return new Command(Command.CommandType.STORE) {
					public Object getResult() {
						return true;
					}

				};
			}

			private Command parseNotStored(ByteBuffer buffer, int notStoredIndex) {
				if (parseLine(buffer, notStoredIndex) == null)
					return null;
				return new Command(Command.CommandType.STORE) {
					public Object getResult() {
						return false;
					}

				};
			}

			private Command parseDeleted(ByteBuffer buffer, int deletedIndex) {
				if (parseLine(buffer, deletedIndex) == null)
					return null;
				return new Command(Command.CommandType.OTHER) {
					public Object getResult() {
						return true;
					}

				};
			}

			private Command parseNotFound(ByteBuffer buffer, int notFoundIndex) {
				if (parseLine(buffer, notFoundIndex) == null)
					return null;
				return new Command(Command.CommandType.OTHER) {
					public Object getResult() {
						return false;
					}

				};
			}

			private Command parseException(ByteBuffer buffer, int errorIndex) {
				if (parseLine(buffer, errorIndex) == null)
					return null;
				return new Command(Command.CommandType.EXCEPTION) {
					@Override
					public RuntimeException getException() {
						return new MemcachedException("unknown command");
					}

				};
			}

			private Command parseClientException(ByteBuffer buffer,
					int clientErrorIndex) {
				byte[] bytes = null;
				if ((bytes = parseLine(buffer, clientErrorIndex)) == null)
					return null;
				String[] items = new String(bytes).split(" ");
				final String error = items.length > 1 ? items[1]
						: "unknown client error";
				return new Command(Command.CommandType.EXCEPTION) {
					@Override
					public RuntimeException getException() {
						return new MemcachedClientException(error);
					}

				};
			}

			private Command parseServerException(ByteBuffer buffer,
					int serverErrorIndex) {
				byte[] bytes = null;
				if ((bytes = parseLine(buffer, serverErrorIndex)) == null)
					return null;
				String[] items = new String(bytes).split(" ");
				final String error = items.length > 1 ? items[1]
						: "unknown server error";
				return new Command() {
					@Override
					public RuntimeException getException() {
						return new MemcachedServerException(error);
					}

				};
			}

			private Command parseVersionCommand(ByteBuffer buffer,
					int versionIndex) {
				byte[] bytes = null;
				if ((bytes = parseLine(buffer, versionIndex)) == null)
					return null;
				String[] items = new String(bytes).split(" ");
				final String version = items.length > 1 ? items[1]
						: "unknown version";
				return new Command() {

					@Override
					public Object getResult() {
						return version;
					}

				};
			}

			private Command parseIncrDecrCommand(ByteBuffer buffer, int origPos) {
				final byte[] bytes;
				if ((bytes = parseLine(buffer, origPos)) == null)
					return null;
				return new Command(Command.CommandType.OTHER) {
					@Override
					public Object getResult() {
						return Integer.parseInt(new String(bytes));
					}

				};
			}

			private byte[] parseLine(ByteBuffer buffer, int offset) {
				int index = ByteBufferUtils.kmpIndexOf(buffer, SPLIT_PATTERN, offset);
				if (index >= 0) {
					int limit = buffer.limit();
					buffer.limit(index);
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
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
