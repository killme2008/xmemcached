/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.rubyeye.xmemcached;

import java.nio.ByteBuffer;

/**
 *
 * @author dennis
 */
public interface MemcachedProtocolHandler {

    boolean onReceive(ByteBuffer buffer);

}
