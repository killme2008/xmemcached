package net.rubyeye.xmemcached.command.text;

import java.util.List;
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
		if (mergeCount < 0) {
			// single get
			if (this.returnValues.get(this.getKey()) == null) {
				if (!wasFirst)
					decodeError();
				else
					this.countDownLatch();
			} else {
				CachedData data = this.returnValues.get(this.getKey());
				setResult(data);
				this.countDownLatch();
			}
		} else {
			// merge get
			List<Command> mergeCommands = getMergeCommands();
			getIoBuffer().free();
			for (Command nextCommand : mergeCommands) {
				CachedData data = this.returnValues.get(nextCommand.getKey());
				nextCommand.setResult(data);
				nextCommand.countDownLatch();
			}
		}
	}
}
