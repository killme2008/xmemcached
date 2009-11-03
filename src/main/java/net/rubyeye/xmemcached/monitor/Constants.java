package net.rubyeye.xmemcached.monitor;

/**
 * Constants
 * 
 * @author dennis
 * 
 */
public class Constants {
	/**
	 * Whether to enable client statisitics
	 */
	public static final String XMEMCACHED_STATISTICS_ENABLE = "xmemcached.statistics.enable";
	/**
	 * JMX RMI service name
	 */
	public static final String XMEMCACHED_RMI_NAME = "xmemcached.rmi.name";
	/**
	 * JMX RMI port
	 */
	public static final String XMEMCACHED_RMI_PORT = "xmemcached.rmi.port";
	/**
	 * Whether to enable jmx supports
	 */
	public static final String XMEMCACHED_JMX_ENABLE = "xmemcached.jmx.enable";
	public static final byte[] CRLF = { '\r', '\n' };
	public static final byte[] GET = { 'g', 'e', 't' };
	public static final byte[] GETS = { 'g', 'e', 't', 's' };
	public static final byte SPACE = ' ';

	/**
	 * Max session read buffer size,758k
	 */
	public static final int MAX_SESSION_READ_BUFFER_SIZE = 768 * 1024;
	public static final String NO_REPLY = "noreply".intern();
}
