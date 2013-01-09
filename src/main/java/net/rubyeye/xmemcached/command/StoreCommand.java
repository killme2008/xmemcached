package net.rubyeye.xmemcached.command;

/**
 * A store command interface for STORE commands such as SET,ADD
 * 
 * @author apple
 * 
 */
public interface StoreCommand {

	public void setValue(Object value);

	public Object getValue();
}
