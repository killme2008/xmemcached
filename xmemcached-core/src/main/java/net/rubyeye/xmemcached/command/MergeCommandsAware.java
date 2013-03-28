package net.rubyeye.xmemcached.command;

import java.util.Map;

/**
 * Merge commands aware interface.Merge commands mean that merge get commands to
 * a bulk-get commands.
 * 
 * @author boyan
 * 
 */
public interface MergeCommandsAware {

	public Map<Object, Command> getMergeCommands();

	public void setMergeCommands(Map<Object, Command> mergeCommands);

}
