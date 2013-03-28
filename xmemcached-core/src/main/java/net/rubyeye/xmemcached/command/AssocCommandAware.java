package net.rubyeye.xmemcached.command;

import java.util.List;

/**
 * Assoc commands aware interface.Association commands mean that commands has
 * the same key.
 * 
 * @author dennis
 * 
 */
public interface AssocCommandAware {
	public List<Command> getAssocCommands();

	public void setAssocCommands(List<Command> assocCommands);
}
