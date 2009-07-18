package net.rubyeye.xmemcached.command;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.transcoders.Transcoder;

public abstract class StoreCommand extends Command {
	protected int expTime;
	protected long cas;
	protected Object value;
	@SuppressWarnings("unchecked")
	public StoreCommand(String key, byte[] keyBytes, CommandType cmdType,
			CountDownLatch latch, int exp, long cas, Object value,
			boolean noreply, Transcoder transcoder) {
		super(key, keyBytes, cmdType, latch);
		this.expTime = exp;
		this.cas = cas;
		this.value = value;
		this.noreply = noreply;
		this.transcoder = transcoder;
	}

	public final int getExpTime() {
		return this.expTime;
	}

	public final void setExpTime(int exp) {
		this.expTime = exp;
	}

	public final long getCas() {
		return this.cas;
	}

	public final void setCas(long cas) {
		this.cas = cas;
	}

	public final Object getValue() {
		return this.value;
	}

	public final void setValue(Object value) {
		this.value = value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Transcoder getTranscoder() {
		return this.transcoder;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

}
