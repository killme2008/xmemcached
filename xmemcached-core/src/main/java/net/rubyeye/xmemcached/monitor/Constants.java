/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.monitor;

import java.util.concurrent.atomic.AtomicInteger;

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
	 * 
	 */
	public static final String XMEMCACHED_RMI_NAME = "xmemcached.rmi.name";
	/**
	 * JMX RMI port
	 * 
	 * 
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
	public static final byte[] INCR = { 'i', 'n', 'c', 'r' };
	public static final byte[] DECR = { 'd', 'e', 'c', 'r' };
	public static final byte[] DELETE = { 'd', 'e', 'l', 'e', 't', 'e' };
	public static final byte[] TOUCH = { 't', 'o', 'u', 'c', 'h' };
	/**
	 * Max session read buffer size,758k
	 */
	public static final int MAX_SESSION_READ_BUFFER_SIZE = 768 * 1024;
	public static final byte[] NO_REPLY = { 'n', 'o', 'r', 'e', 'p', 'l', 'y' };
	/**
	 * Client instance counter
	 */
	public static final AtomicInteger MEMCACHED_CLIENT_COUNTER = new AtomicInteger(
			0);
}
