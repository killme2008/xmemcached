package net.rubyeye.xmemcached.command;

import java.util.List;

public interface AssocCommandAware {
	public List<Command> getAssocCommands();

	public void setAssocCommands(List<Command> assocCommands);
}
