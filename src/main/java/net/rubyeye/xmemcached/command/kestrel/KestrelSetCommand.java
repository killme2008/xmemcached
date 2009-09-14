package net.rubyeye.xmemcached.command.kestrel;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.text.TextStoreCommand;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class KestrelSetCommand extends TextStoreCommand {

	@SuppressWarnings("unchecked")
	public KestrelSetCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch, exp, cas, value, noreply,
				transcoder);
		// TODO Auto-generated constructor stub
	}

	@Override
	@SuppressWarnings("unchecked")
	protected CachedData encodeValue() {

		final CachedData value = this.transcoder.encode(this.value);

		int flags = value.getFlag();
		byte[] flagBytes = KestrelGetCommand.transcoderUtils.encodeInt(flags);
		byte[] origData = value.getData();
		byte[] newData = new byte[origData.length + 4];
		System.arraycopy(flagBytes, 0, newData, 0, 4);
		System.arraycopy(origData, 0, newData, 4, origData.length);
		value.setCapacity(newData.length);
		value.setData(newData);
		return value;
	}

}
