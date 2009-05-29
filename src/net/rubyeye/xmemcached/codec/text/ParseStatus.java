package net.rubyeye.xmemcached.codec.text;

/**
 * 解析状态
 *
 * @author dennis
 *
 */
public enum ParseStatus {

	NULL, GET, END, STORED, NOT_STORED, ERROR, CLIENT_ERROR, SERVER_ERROR, DELETED, NOT_FOUND, VERSION, INCR, EXISTS, STATS;
}