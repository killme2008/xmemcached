package net.rubyeye.xmemcached.command;

import java.util.Map;

public interface MergeCommandsAware {

	public Map<Object, Command> getMergeCommands();

	public void setMergeCommands(Map<Object, Command> mergeCommands);

}
