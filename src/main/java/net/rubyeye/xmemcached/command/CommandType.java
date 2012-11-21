package net.rubyeye.xmemcached.command;

/**
 * Command Type for memcached protocol.
 * 
 * @author dennis
 * 
 */
public enum CommandType {

	NOOP, STATS, FLUSH_ALL, GET_ONE, GET_MANY, SET, REPLACE, ADD, EXCEPTION, DELETE, VERSION, QUIT, INCR, DECR, GETS_ONE, GETS_MANY, CAS, APPEND, PREPEND, GET_HIT, GET_MISS, VERBOSITY, AUTH_LIST, AUTH_START, AUTH_STEP, TOUCH, GAT, GATQ, SET_MANY;

}