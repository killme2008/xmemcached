package net.rubyeye.xmemcached.command.text;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.CachedData;

public class TextGetOneCommand extends TextGetCommand {

	public TextGetOneCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch) {
		super(key, keyBytes, cmdType, latch);
	}

	@Override
	public void dispatch() {
		if (this.mergeCount < 0) {
			// single get
			if (this.returnValues.get(this.getKey()) == null) {
				if (!this.wasFirst) {
					decodeError();
				} else {
					this.countDownLatch();
				}
			} else {
				CachedData data = this.returnValues.get(this.getKey());
				setResult(data);
				this.countDownLatch();
			}
		} else {
			// merge get
			Collection<Command> mergeCommands = getMergeCommands().values();
			getIoBuffer().free();
			for (Command nextCommand : mergeCommands) {
				TextGetCommand textGetCommand = (TextGetCommand) nextCommand;
				textGetCommand.countDownLatch();
				if (textGetCommand.getAssocCommands() != null) {
					for (Command assocCommand : textGetCommand
							.getAssocCommands()) {
						assocCommand.countDownLatch();
					}
				}
			}
		}
	}
}
