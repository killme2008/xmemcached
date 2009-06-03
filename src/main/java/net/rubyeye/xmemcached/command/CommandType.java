package net.rubyeye.xmemcached.command;

/**
 * 命令类型
 * 
 * @author dennis
 * 
 */
public enum CommandType {

	STATS, FLUSH_ALL, GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION, DELETE, VERSION, INCR, DECR, GETS_ONE, GETS_MANY, CAS, APPEND, PREPEND, GET_HIT, GET_MISS;
}