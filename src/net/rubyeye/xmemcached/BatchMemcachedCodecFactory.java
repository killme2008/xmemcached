package net.rubyeye.xmemcached;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.yanf4j.nio.CodecFactory;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedClientException;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.exception.MemcachedServerException;
import net.rubyeye.xmemcached.utils.ByteBufferMatcher;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.CachedData;
@SuppressWarnings("unchecked")
public class BatchMemcachedCodecFactory implements CodecFactory {
	private static final int MERGE_COUNT = 100;

	private static final ByteBuffer SPLIT = ByteBuffer.wrap(Command.SPLIT
			.getBytes());

	protected static final Log log = LogFactory
			.getLog(BatchMemcachedCodecFactory.class);
	/**
	 * 
	 * private static final ByteBufferPattern SPLIT_PATTERN = ByteBufferPattern
	 * .compile(SPLIT);
	 */

	static ByteBufferMatcher SPLIT_MATCHER = new ByteBufferMatcher(SPLIT);

	enum ParseStatus {
		NULL, GET, END, STORED, NOT_STORED, ERROR, CLIENT_ERROR, SERVER_ERROR, DELETED, NOT_FOUND, VERSION, INCR;
	}

	@SuppressWarnings("unchecked")
	public Decoder getDecoder() {
		return new Decoder() {
			private Command resultCommand;
			private ParseStatus status = ParseStatus.NULL;
			private List<byte[]> currentLines = null;
			private String currentLine = null;
			private List<Command> commands;

			private void prevLine(ByteBuffer buffer) {
				if (this.currentLine != null) {
					byte[] lineBytes = ByteUtils.getBytes(this.currentLine);
					buffer.position(buffer.position() - lineBytes.length - 2);
					this.currentLine = null;
				}
			}

			public Object decode(ByteBuffer buffer) {
				if (commands == null)
					commands = new ArrayList<Command>(MERGE_COUNT);
				LABEL: while (true) {
					if (commands.size() >= MERGE_COUNT) {
						return checkCommands();
					}
					switch (this.status) {
					case NULL:
						nextLine(buffer);
						if (currentLine == null) {
							return checkCommands();
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
						} else if (currentLine.equals("STORED")) {
							this.status = ParseStatus.STORED;
						} else if (currentLine.equals("DELETED")) {
							this.status = ParseStatus.DELETED;
						} else if (currentLine.equals("END")) {
							this.status = ParseStatus.END;
						} else if (currentLine.equals("NOT_STORED")) {
							this.status = ParseStatus.NOT_STORED;
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
						else {
							log.error("unknow response:" + this.currentLine);
							throw new IllegalStateException("unknown response:"
									+ this.currentLine);
						}
					case GET:
						List<String> keys = (List<String>) this.resultCommand
								.getKey();
						List<CachedData> datas = (List<CachedData>) this.resultCommand
								.getResult();
						while (true) {
							nextLine(buffer);
							if (currentLine == null)
								return checkCommands();
							if (this.currentLine.equals("END")) {
								commands.add(this.resultCommand);
								this.resultCommand = null;
								reset();
								continue LABEL;
							} else if (currentLine.startsWith("VALUE")) {
								String[] items = this.currentLine.split(" ");
								int flag = Integer.parseInt(items[2]);
								int dataLen = Integer.parseInt(items[3]);
								// 不够数据
								if (currentLines == null
										|| currentLines.size() == 0) {
									prevLine(buffer);
									this.currentLine = null;
									return checkCommands();
								}
								keys.add(items[1]);
								byte[] data = currentLines.remove(0);
								datas.add(new CachedData(flag, data, dataLen));
								this.currentLine = null;
							} else {
								prevLine(buffer);
								this.currentLine = null;
								return checkCommands();
							}

						}
					case END:
						commands.add(parseEndCommand());
						break;
					case STORED:
						commands.add(parseStored());
						break;
					case NOT_STORED:
						commands.add(parseNotStored());
						break;
					case DELETED:
						commands.add(parseDeleted());
						break;
					case NOT_FOUND:
						commands.add(parseNotFound());
						break;
					case ERROR:
						commands.add(parseException());
						break;
					case CLIENT_ERROR:
						commands.add(parseClientException());
						break;
					case SERVER_ERROR:
						commands.add(parseServerException());
						break;
					case VERSION:
						commands.add(parseVersionCommand());
						break;
					case INCR:
						commands.add(parseIncrDecrCommand());
						break;
					default:
						return checkCommands();
					}
				}
			}

			private Object checkCommands() {
				List<Command> returnCommands = this.commands;
				this.commands = null;
				if (returnCommands.size() > 0)
					return returnCommands;

				else
					return null;
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
				if (this.currentLines == null || this.currentLines.size() == 0) {
					List<Integer> list = SPLIT_MATCHER.matchAll(buffer);
					if (list == null || list.size() == 0)
						return;
					this.currentLines = new LinkedList<byte[]>();
					Iterator<Integer> it = list.iterator();
					while (it.hasNext()) {
						int nextPos = it.next();
						byte[] bytes = new byte[nextPos - buffer.position()];
						buffer.get(bytes);
						currentLines.add(bytes);
						buffer.position(nextPos + SPLIT.remaining());
					}
					try {
						this.currentLine = new String(this.currentLines
								.remove(0), "utf-8");
						return;
					} catch (UnsupportedEncodingException e) {

					}
				} else if (this.currentLines.size() > 0) {
					try {
						this.currentLine = new String(this.currentLines
								.remove(0), "utf-8");
						return;
					} catch (UnsupportedEncodingException e) {

					}
				}
			}

		};
	}

	public Encoder getEncoder() {
		return new Encoder() {
			public ByteBuffer[] encode(Object cmd) {
				return new ByteBuffer[] { ((Command) cmd).getByteBuffer() };
			}

		};
	}
}
