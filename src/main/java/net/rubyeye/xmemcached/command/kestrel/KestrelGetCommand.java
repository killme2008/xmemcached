package net.rubyeye.xmemcached.command.kestrel;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextGetCommand;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.TranscoderUtils;

public class KestrelGetCommand extends TextGetCommand {

	public KestrelGetCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch) {
		super(key, keyBytes, cmdType, latch);
	}

	public static TranscoderUtils transcoderUtils = new TranscoderUtils(false);

	@Override
	public void dispatch() {
		if (this.returnValues.size() == 0) {
			if (!this.wasFirst) {
				decodeError();
			} else {
				this.countDownLatch();
			}
		} else {
			CachedData value = this.returnValues.values().iterator().next();
			byte[] data = value.getData();
			if (data.length >= 4) {
				byte[] flagBytes = new byte[4];
				System.arraycopy(data, 0, flagBytes, 0, 4);
				byte[] realData = new byte[data.length - 4];
				System.arraycopy(data, 4, realData, 0, data.length - 4);
				int flag = this.transcoderUtils.decodeInt(flagBytes);
				value.setFlag(flag);
				value.setData(realData);
				value.setCapacity(realData.length);
			}
			setResult(value);
			this.countDownLatch();
		}
	}

}
