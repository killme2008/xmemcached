package net.rubyeye.xmemcached.command;

import java.util.Map;

import net.rubyeye.xmemcached.transcoders.CachedData;

public interface MapReturnValueAware {

	public abstract Map<String, CachedData> getReturnValues();

}