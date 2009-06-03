package net.rubyeye.xmemcached.command.text;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class TextGetMultiCommand extends TextGetCommand {

	@SuppressWarnings("unchecked")
	public TextGetMultiCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch,Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch);
		this.transcoder = transcoder;
	}

	@Override
	public final void dispatch() {
		setResult(this.returnValues);
		countDownLatch();
	}
}
