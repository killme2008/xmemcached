package net.rubyeye.xmemcached.monitor;

/**
 * 属性名称
 * 
 * @author dennis
 * 
 */
public class Constants {
	/**
	 * 是否启用统计
	 */
	public static final String XMEMCACHED_STATISTICS_ENABLE = "xmemcached.statistics.enable";
	/**
	 * 通过RMI发布的服务名称
	 */
	public static final String XMEMCACHED_RMI_NAME = "xmemcached.rmi.name";
	/**
	 * RMI端口
	 */
	public static final String XMEMCACHED_RMI_PORT = "xmemcached.rmi.port";
	/**
	 * 是否启用JMX
	 */
	public static final String XMEMCACHED_JMX_ENABLE = "xmemcached.jmx.enable";
	public static final byte[] CRLF = { '\r', '\n' };
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't','s' };
	public static final byte SPACE = ' ';

	public static final int MAX_SESSION_READ_BUFFER_SIZE = 768 * 1024;
	public static final String NO_REPLY = "noreply".intern();
}
