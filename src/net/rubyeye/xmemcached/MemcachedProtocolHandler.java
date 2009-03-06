/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.rubyeye.xmemcached;

import java.nio.ByteBuffer;

/**
 * memcached协议解析接口
 * 
 * @author dennis
 */
public interface MemcachedProtocolHandler {

	boolean onReceive(ByteBuffer buffer);

}
