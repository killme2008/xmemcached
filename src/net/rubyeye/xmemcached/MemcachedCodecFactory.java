package net.rubyeye.xmemcached;

import java.io.UnsupportedEncodingException;
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
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.CachedData;

public class MemcachedCodecFactory implements CodecFactory<Command> {
	private static final ByteBuffer SPLIT = ByteBuffer.wrap(Command.SPLIT
			.getBytes());

	private static final ByteBufferPattern SPLIT_PATTERN = ByteBufferPattern
			.compile(SPLIT);

	enum ParseStatus {
		NULL, GET, END, STORED, NOT_STORED, ERROR, CLIENT_ERROR, SERVER_ERROR, DELETED, NOT_FOUND, VERSION, INCR;
	}

	@SuppressWarnings("unchecked")
	public Decoder<Command> getDecoder() {
		return new Decoder<Command>() {
			private Command resultCommand;
			private ParseStatus status = ParseStatus.NULL;
			private String currentLine = null;

			public Command decode(ByteBuffer buffer) {
				int origPos = buffer.position();
				int origLimit = buffer.limit();
				LABEL: while (true) {
					switch (this.status) {
					case NULL:
						nextLine(buffer);
						if (currentLine == null) {
							return null;
						}
						if (currentLine.startsWith("VALUE")) {
							this.resultCommand = new Command(
									Command.CommandType.GET);
							List<String> keys = new ArrayList<String>(30);
							this.resultCommand.setKey(keys);
							List<CachedData> datas = new ArrayList<CachedData>(
									30);
							this.resultCommand.setResult(datas);
							this.status = ParseStatus.GET;
						} else if (currentLine.equals("END")) {
							this.status = ParseStatus.END;
						} else if (currentLine.equals("STORED")) {
							this.status = ParseStatus.STORED;
						} else if (currentLine.equals("NOT_STORED")) {
							this.status = ParseStatus.NOT_STORED;
						} else if (currentLine.equals("DELETED")) {
							this.status = ParseStatus.DELETED;
						} else if (currentLine.equals("NOT_FOUND")) {
							this.status = ParseStatus.NOT_FOUND;
						} else if (currentLine.equals("ERROR")) {
							this.status = ParseStatus.ERROR;
						} else if (currentLine.startsWith("CLIENT_ERROR")) {
							this.status = ParseStatus.CLIENT_ERROR;
						} else if (currentLine.startsWith("SERVER_ERROR")) {
							this.status = ParseStatus.SERVER_ERROR;
						} else if (currentLine.startsWith("VERSION ")) {
							this.status = ParseStatus.VERSION;
						} else {
							this.status = ParseStatus.INCR;
						}
						if (!this.status.equals(ParseStatus.NULL))
							continue LABEL;
						else
							return null;
					case GET:
						List<String> keys = (List<String>) this.resultCommand
								.getKey();
						List<CachedData> datas = (List<CachedData>) this.resultCommand
								.getResult();
						while (true) {
							nextLine(buffer);
							if (currentLine == null)
								return null;
							if (this.currentLine.equals("END")) {
								Command returnCommand = this.resultCommand;
								this.resultCommand = null;
								reset();
								return returnCommand;
							} else if (currentLine.startsWith("VALUE")) {
								String[] items = this.currentLine.split(" ");
								int flag = Integer.parseInt(items[2]);
								int dataLen = Integer.parseInt(items[3]);
								// 数据不完整
								if (buffer.remaining() < dataLen + 2) {
									prevLine(buffer);
									buffer.position(origPos).limit(origLimit);
									this.currentLine = null;
									return null;
								}
								keys.add(items[1]);
								byte[] data = new byte[dataLen];
								buffer.get(data);
								datas.add(new CachedData(flag, data, dataLen));
								buffer.position(buffer.position()
										+ SPLIT.remaining());
								this.currentLine = null;
							} else {
								prevLine(buffer);
								buffer.position(origPos).limit(origLimit);
								this.currentLine = null;
								return null;
							}

						}
					case END:
						return parseEndCommand();
					case STORED:
						return parseStored();
					case NOT_STORED:
						return parseNotStored();
					case DELETED:
						return parseDeleted();
					case NOT_FOUND:
						return parseNotFound();
					case ERROR:
						return parseException();
					case CLIENT_ERROR:
						return parseClientException();
					case SERVER_ERROR:
						return parseServerException();
					case VERSION:
						return parseVersionCommand();
					case INCR:
						return parseIncrDecrCommand();
					default:
						return null;

					}
				}
			}

			private void prevLine(ByteBuffer buffer) {
				int nowPos;
				nowPos = buffer.position();
				byte[] lineBytes = ByteUtils.getBytes(this.currentLine);
				int prevPos = nowPos - lineBytes.length - 2;
				buffer.position(nowPos - lineBytes.length - 2);
				buffer.put(lineBytes).put(SPLIT.array());
				buffer.position(prevPos);
				this.currentLine = null;
			}

			private Command parseEndCommand() {
				reset();
				return new Command(Command.CommandType.GET);
			}

			private Command parseStored() {
				reset();
				return new Command(Command.CommandType.STORE) {
					public Object getResult() {
						return true;
					}

				};
			}

			private Command parseNotStored() {
				reset();
				return new Command(Command.CommandType.STORE) {
					public Object getResult() {
						return false;
					}

				};
			}

			private Command parseDeleted() {
				reset();
				return new Command(Command.CommandType.OTHER) {
					public Object getResult() {
						return true;
					}

				};
			}

			private Command parseNotFound() {
				reset();
				return new Command(Command.CommandType.OTHER) {
					public Object getResult() {
						return false;
					}

				};
			}

			private Command parseException() {
				reset();
				return new Command(Command.CommandType.EXCEPTION) {
					@Override
					public RuntimeException getException() {
						return new MemcachedException("unknown command");
					}

				};
			}

			private Command parseClientException() {
				String[] items = this.currentLine.split(" ");
				final String error = items.length > 1 ? items[1]
						: "unknown client error";
				reset();
				return new Command(Command.CommandType.EXCEPTION) {
					@Override
					public RuntimeException getException() {
						return new MemcachedClientException(error);
					}

				};
			}

			private Command parseServerException() {
				String[] items = this.currentLine.split(" ");
				final String error = items.length > 1 ? items[1]
						: "unknown server error";
				reset();
				return new Command() {
					@Override
					public RuntimeException getException() {
						return new MemcachedServerException(error);
					}

				};
			}

			private Command parseVersionCommand() {
				String[] items = this.currentLine.split(" ");
				final String version = items.length > 1 ? items[1]
						: "unknown version";
				reset();
				return new Command() {

					@Override
					public Object getResult() {
						return version;
					}

				};
			}

			private void reset() {
				this.status = ParseStatus.NULL;
				this.currentLine = null;
			}

			private Command parseIncrDecrCommand() {
				final Integer result = Integer.parseInt(this.currentLine);
				reset();
				return new Command(Command.CommandType.OTHER) {
					@Override
					public Object getResult() {
						return result;
					}

				};
			}

			private void nextLine(ByteBuffer buffer) {
				if (this.currentLine != null)
					return;
				int index = ByteBufferUtils.kmpIndexOf(buffer, SPLIT_PATTERN);
				if (index >= 0) {
					int limit = buffer.limit();
					buffer.limit(index);
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					buffer.limit(limit);
					buffer.position(index + SPLIT.remaining());
					try {
						this.currentLine = new String(bytes, "utf-8");
					} catch (UnsupportedEncodingException e) {

					}

				} else
					this.currentLine = null;
			}

		};
	}

	public Encoder<Command> getEncoder() {
		return new Encoder<Command>() {
			public ByteBuffer[] encode(Command cmd) {
				return new ByteBuffer[] { cmd.getCmd() };
			}

		};
	}
}
