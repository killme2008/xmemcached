package net.rubyeye.xmemcached.command;

import java.util.Map;

import net.rubyeye.xmemcached.transcoders.CachedData;

/**
 * Command which implement this interface,it's return value is a map
 * 
 * @author dennis
 * 
 */
public interface MapReturnValueAware {

	public abstract Map<String, CachedData> getReturnValues();

}